package com.petermarshall.database.datasource;

import com.petermarshall.DateHelper;
import com.petermarshall.database.tables.MatchTable;
import com.petermarshall.scrape.classes.*;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;

public class DS_Update {
    public static void updateGamesInDB(League league, Season season) {
        try (Statement batchStatement = DS_Main.connection.createStatement()) {
            Date lastMatchInDb = DateHelper.createDateFromSQL(DS_Get.getLastCompletedMatchInLeague(league));
            Date matchLimit = DateHelper.subtractXDaysFromDate(lastMatchInDb, 7);

            int leagueId = DS_Get.getLeagueId(league);
            int seasonYearStart = season.getSeasonYearStart();
            HashMap<String, Integer> teamIds = DS_Insert.getTeamIds(season.getAllTeams(), leagueId);
            season.getAllMatches().forEach(match -> {
                if (match.getKickoffTime().after(matchLimit)) {
                    int homeTeamId = teamIds.get(match.getHomeTeam().getTeamName());
                    int awayTeamId = teamIds.get(match.getAwayTeam().getTeamName());
                    int matchId = DS_Get.getMatchId(homeTeamId, awayTeamId, seasonYearStart);

                    try {
                        batchStatement.addBatch("UPDATE " + MatchTable.getTableName() +
                                " SET " + MatchTable.getColHomeXg() + " = " + match.getHomeXGF() + ", " + MatchTable.getColAwayXg() + " = " + match.getAwayXGF() + ", " +
                                MatchTable.getColHomeScore() + " = " + match.getHomeScore() + ", " + MatchTable.getColAwayScore() + " = " + match.getAwayScore() + ", " +
                                MatchTable.getColHomeWinOdds() + " = " + match.getHomeOdds() + ", " + MatchTable.getColDrawOdds() + " = " + match.getDrawOdds() + ", " +
                                MatchTable.getColAwayWinOdds() + " = " + match.getAwayOdds() + ", " + MatchTable.getColFirstScorer() + " = " + match.getFirstScorer() + ", " +
                                MatchTable.getColDate() + " = '" + match.getKickoffTime() + "'" +
                                " WHERE _id = " + matchId);
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }

                    DS_Insert.addPlayerRatingsToBatch(batchStatement, match.getHomePlayerRatings(), matchId, homeTeamId);
                    DS_Insert.addPlayerRatingsToBatch(batchStatement, match.getAwayPlayerRatings(), matchId, awayTeamId);
                }
            });
            batchStatement.executeBatch();
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("It's possible that our query to the database didn't return a result and therefore the resultset closes automatically, throwing this exception.");
            e.printStackTrace();
        }
    }

    /*
     * Needed because understat (who we created our dates from) sometimes have incorrect start dates and times - called from SofaScore file.
     * Will also be used to change postponed matches in the database.
     * Should only be called for todays games. will not be going through the whole seasons games calling this func.
     */
    public static void updateKickoffTime(int seasonYearStart, String homeTeamName, String awayTeamName, String startDate, int leagueId) {
        try (Statement statement = DS_Main.connection.createStatement()) {
            int homeTeamId = DS_Get.getTeamId(homeTeamName, leagueId);
            int awayTeamId = DS_Get.getTeamId(awayTeamName, leagueId);

            statement.execute("UPDATE " + MatchTable.getTableName() +
                    " SET " + MatchTable.getColDate() + " = '" + startDate +
                    "' WHERE " + MatchTable.getColHometeamId() + " = " + homeTeamId +
                    " AND " + MatchTable.getColAwayteamId() + " = " + awayTeamId +
                    " AND " + MatchTable.getColSeasonYearStart() + " = " + seasonYearStart);
        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
