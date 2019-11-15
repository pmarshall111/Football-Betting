package com.petermarshall.database;

import com.petermarshall.*;
import com.petermarshall.logging.MatchLog;
import com.petermarshall.machineLearning.createData.classes.MatchToPredict;
import com.petermarshall.database.tables.*;
import com.petermarshall.mail.UptimeData;
import com.petermarshall.scrape.classes.*;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;

/*
 * IMPORTANT: To get data, we must first open the connection and once we're done we should close the connection.
 * Class used to read, write and update data to database.
 */
public class DataSource {
    private static final String CONNECTION_NAME = "jdbc:sqlite:C:\\Databases\\footballMatchesREFACTOR.db";
    private static Connection connection;

    private static int LEAGUE_NEXT_ID = -1;
    private static int SEASON_NEXT_ID = -1;
    private static int TEAM_NEXT_ID = -1;
    private static int MATCH_NEXT_ID = -1;
    private static int PLAYER_RATING_NEXT_ID = -1;
    private static int PLAYER_NEXT_ID = -1;

    private static String HOMETEAM = "homeTeam";
    private static String AWAYTEAM = "awayTeam";

    public static boolean isOpen() {
        try {
        return connection == null || !connection.isClosed();
        } catch(SQLException e) {
            return false;
        }
    }


    /*
     * To be called before every use of this class, followed by a call to closeConnection when all db
     * work has been completed.
     */
    public static boolean openConnection() {
        try {
            connection = DriverManager.getConnection(CONNECTION_NAME);
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }
    public static boolean closeConnection() {
        try {
            connection.close();
            return true;
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    /*
     * To be called when first creating database and also before any inserts to the database. Creates all tables for database and
     * gets the next index of each table that can be added.
     * Returns true if database initialising was successful. Otherwise will return false.
     */
    public static boolean initDB() {
        try (Statement statement = connection.createStatement()) {

            statement.execute("CREATE TABLE IF NOT EXISTS '" + LeagueTable.getTableName() +
                    "' ('" + LeagueTable.getColName() + "' TEXT NOT NULL UNIQUE," + " '_id' INTEGER UNIQUE, PRIMARY KEY('_id') )");

            statement.execute("CREATE TABLE IF NOT EXISTS '" + SeasonTable.getTableName() +
                    "' ('" + SeasonTable.getColYearBeginning() + "' INTEGER NOT NULL, '_id' INTEGER UNIQUE, PRIMARY KEY('_id') )");

            statement.execute("CREATE TABLE IF NOT EXISTS '" + TeamTable.getTableName() +
                    "' ('" + TeamTable.getColTeamName() + "' TEXT NOT NULL, '" + TeamTable.getColLeagueId() + "' INTEGER NOT NULL, "
                    + "'_id' INTEGER NOT NULL, PRIMARY KEY('_id'), " +
                    "FOREIGN KEY('" + TeamTable.getColLeagueId() + "') REFERENCES '" + LeagueTable.getTableName() + "'('_id'))");

            statement.execute("CREATE TABLE IF NOT EXISTS '" + MatchTable.getTableName() +
                    "' ('" + MatchTable.getColHomeScore() + "' INTEGER, '" + MatchTable.getColAwayScore() + "' INTEGER, '" +
                    MatchTable.getColHomeXg() + "' REAL, '" + MatchTable.getColAwayXg() + "' REAL, '" +
                    MatchTable.getColDate() + "' TEXT NOT NULL, '" + MatchTable.getColHomeWinOdds() + "' REAL, '" +
                    MatchTable.getColDrawOdds() + "' REAL, '" + MatchTable.getColAwayWinOdds() + "' REAL, '" +
                    MatchTable.getColFirstScorer() + "' INTEGER, '" + MatchTable.getColIsPostponed() + "' INTEGER DEFAULT 0, '" +
                    MatchTable.getColHometeamId() + "' INTEGER NOT NULL, '" + MatchTable.getColAwayteamId() + "' INTEGER NOT NULL, '" +
                    MatchTable.getColSeasonId() + "' INTEGER NOT NULL, '" + MatchTable.getColPredictedLive() + "' INTEGER, '" +
                    "CHECK('" + MatchTable.getColPredictedLive() + "' == 1 OR '" + MatchTable.getColPredictedLive() + "' == 2), " +
                    "FOREIGN KEY('" + MatchTable.getColHometeamId() + "') REFERENCES '" + TeamTable.getTableName() + "'('_id'), " +
                    "FOREIGN KEY('" + MatchTable.getColAwayteamId() + "') REFERENCES '" + TeamTable.getTableName() + "'('_id'), " +
                    "PRIMARY KEY('_id'))");

            statement.execute("CREATE TABLE IF NOT EXISTS '" + PlayerRatingTable.getTableName() +
                    "' ('" + PlayerRatingTable.getColMins() + "' INTEGER NOT NULL, " + "CHECK ('" + PlayerRatingTable.getColMins() + "' <= 90 " +
                    " AND '" + PlayerRatingTable.getColMins() + "' > 0), '" +
                    PlayerRatingTable.getColRating() + "' REAL NOT NULL, " + "CHECK ('" + PlayerRatingTable.getColRating() + "' <= 10 " +
                    "AND '" + PlayerRatingTable.getColRating() + "' > 0), '" + PlayerRatingTable.getColPlayerId() + "' INTEGER NOT NULL, '" +
                    PlayerRatingTable.getColMatchId() + "' INTEGER NOT NULL, '" + PlayerRatingTable.getColTeamId() + "' INTEGER NOT NULL, " +
                    "FOREIGN KEY('" + PlayerRatingTable.getColPlayerId() + "') REFERENCES '" + PlayerTable.getTableName() + "'('_id'), " +
                    "FOREIGN KEY('" + PlayerRatingTable.getColTeamId() + "') REFERENCES '" + TeamTable.getTableName() + "'('_id'), " +
                    "FOREIGN KEY('" + PlayerRatingTable.getColMatchId() + "') REFERENCES '" + MatchTable.getTableName() + "'('_id'), " +
                    "PRIMARY KEY('_id'))");

            statement.execute("CREATE TABLE IF NOT EXISTS '" + PlayerTable.getTableName() +
                    "' ('" + PlayerTable.getColPlayerName() + "' TEXT NOT NULL, '_id' INTEGER NOT NULL, PRIMARY KEY('_id')");

            statement.execute("CREATE TABLE IF NOT EXISTS '" + BetTable.getTableName() +
                    "' ('" + BetTable.getColResultBetOn() + "' INTEGER NOT NULL " +
                    "CHECK('" + BetTable.getColResultBetOn() + "' >= 1 AND '" + BetTable.getColResultBetOn() + "' <= 3), '" +
                    BetTable.getColOdds() + "' REAL NOT NULL, '" + BetTable.getColStake() + "' REAL NOT NULL, '" +
                    BetTable.getColMatchId() + "' INTEGER NOT NULL, " +
                    "FOREIGN KEY('" + BetTable.getColMatchId() + "') REFERENCES '" + MatchTable.getTableName() + "'('_id'))");

            statement.execute("CREATE TABLE IF NOT EXISTS '" + LogTable.getTableName() +
                    "'( '" + LogTable.getColDatetime() + "' TEXT, '" + LogTable.getColInfo() + "' TEXT)");

            return getNextIds();

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /*
     * Retrieves the highest current Id within the current table and sets the class variable to it. Called from the initDb method.
     *
     * Requires separate statements as once you execute a new query on the same statement, the old resultSet is also closed.
     */
    private static boolean getNextIds() {
            try (Statement statement1 = connection.createStatement();
                 Statement statement2 = connection.createStatement();
                 Statement statement3 = connection.createStatement();
                 Statement statement4 = connection.createStatement();
                 Statement statement5 = connection.createStatement();

                 ResultSet LeagueSet = statement1.executeQuery("SELECT max(_id) FROM '" + LeagueTable.getTableName() + "'");
                 ResultSet SeasonSet = statement2.executeQuery("SELECT max(_id) FROM '" + SeasonTable.getTableName() + "'");
                 ResultSet TeamSet = statement3.executeQuery("SELECT max(_id) FROM '" + TeamTable.getTableName() + "'");
                 ResultSet MatchSet = statement4.executeQuery("SELECT max(_id) FROM '" + MatchTable.getTableName() + "'");
                 ResultSet PlayerRatingsSet = statement5.executeQuery("SELECT max(_id) FROM '" + PlayerRatingTable.getTableName() + "'");
            ) {

                LEAGUE_NEXT_ID = LeagueSet.getInt(1);
                SEASON_NEXT_ID = SeasonSet.getInt(1);
                TEAM_NEXT_ID = TeamSet.getInt(1);
                MATCH_NEXT_ID = MatchSet.getInt(1);
                PLAYER_RATING_NEXT_ID = PlayerRatingsSet.getInt(1);

//                System.out.println("League: " + LEAGUE_NEXT_ID + "\nSeason: " + SEASON_NEXT_ID + "\nTeam: " + TEAM_NEXT_ID +
//                        "\nMatch: " + MATCH_NEXT_ID + "\nPlayerRatings: " + PLAYER_RATING_NEXT_ID );

                return true;

            } catch (SQLException e) {
                System.out.println(e.getMessage());
                e.printStackTrace();
                return false;
            }
    }

    /*
     * To be called after first scrape. Puts all data from a league into db. Cannot be called again once league is in database
     * as league table in sqlite has a unique name constraint. To add more data, we must either update existing records or add
     * a new season.
     */
    public static void writeLeagueToDb(League league) {
        if (connection == null) throw new RuntimeException("trying to write data to db when db connection is not open");

        System.out.println("Current primary key: " + LEAGUE_NEXT_ID);

        try (Statement statement = connection.createStatement()) {

            statement.execute("INSERT INTO " + LeagueTable.getTableName() + " (" + LeagueTable.getColName() + ", _id) " +
                    "VALUES ( '" + league.getName() + "', " + ++LEAGUE_NEXT_ID + " )");

            ArrayList<Season> allSeasons = league.getAllSeasons();

            allSeasons.forEach(season -> {
                writeSeasonToDb(statement, season, LEAGUE_NEXT_ID);
            });

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("ERROR writing league " + league.getName() + " to db");

            System.out.println("Error. Primary key now: " + LEAGUE_NEXT_ID);
            e.printStackTrace();
        }
    }


    private static void writeSeasonToDb(Statement statement, Season season, int leagueId) {
        try {
            statement.execute("INSERT INTO " + SeasonTable.getTableName() + " (" + SeasonTable.getColYearBeginning() + ", " + SeasonTable.getColLeagueId() + ", _id) " +
                    "VALUES ( '" + season.getSeasonKey() + "', " + leagueId + ", " + ++SEASON_NEXT_ID + " )");

            ArrayList<Match> matchesInSeason = season.getAllMatches();

            matchesInSeason.forEach(match -> {
                writeMatchToDb(statement, match, SEASON_NEXT_ID);
            });

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("ERROR writing season " + season.getSeasonKey() + " from league " + leagueId + " to db");
            e.printStackTrace();
        }
    }

    /*
     * Writes team to db if the team does not already exist in the database for that season. Otherwise it will find the team in the database.
     * Returns the Team _id for the requested team.
     */
    private static int writeTeamToDbIfNotThere(Statement statement, Team team, int seasonId) {
        try (ResultSet teamQuerySet = statement.executeQuery("SELECT _id FROM " + TeamTable.getTableName() + " " +
                "WHERE " + TeamTable.getColTeamName() + " = '" + team.getTeamName() + "' " +
                "AND " + TeamTable.getColSeasonId() + " = " + seasonId)){

            if (teamQuerySet.next()) {
                return teamQuerySet.getInt(1);
            }
            else {
                statement.execute("INSERT INTO " + TeamTable.getTableName() + " (" + TeamTable.getColTeamName() + ", " + TeamTable.getColSeasonId() + ", _id) " +
                        "VALUES ( '" + team.getTeamName() + "', " + seasonId + ", " + ++TEAM_NEXT_ID + " )");
                return TEAM_NEXT_ID;
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("ERROR writing team " + team.getTeamName() + " from season " + seasonId + " to db");
            e.printStackTrace();
            return -1;
        }
    }

    /*
     * In order to write the match to database, we need to have all fields of our match to have data in. For goals scored and
     * xG, this will generally be -1.
     */
    private static void writeMatchToDb(Statement statement, Match match, int seasonId) {
        try {
            int homeTeamId = writeTeamToDbIfNotThere(statement, match.getHomeTeam(), seasonId);
            int awayTeamId = writeTeamToDbIfNotThere(statement, match.getAwayTeam(), seasonId);

//            System.out.println("INSERT INTO " + MatchTable.getTableName() + " (" + MatchTable.getColDate() + ", " +
//                    MatchTable.getColHometeamId() + ", " + MatchTable.getColAwayteamId() + ", " + MatchTable.getColHomeXg() + ", " + MatchTable.getColAwayXg() + ", " +
//                    MatchTable.getColHomeScore() + ", " + MatchTable.getColAwayScore() + ", " + MatchTable.getColHomeWinOdds() + ", " + MatchTable.getColAwayWinOdds() + ", " +
//                    MatchTable.getColDrawOdds() + ", " + MatchTable.getColFirstScorer() + ", _id) " +
//                    "VALUES ( '" + DateHelper.getSqlDate(match.getKickoffTime()) + "', " + homeTeamId + ", " + awayTeamId + ", " + match.getHomeXGF() + ", " + match.getAwayXGF() + ", " +
//                    match.getHomeScore() + ", " + match.getAwayScore() + ", " + match.getHomeDrawAwayOdds().get(0) + ", " + match.getHomeDrawAwayOdds().get(2) + ", " +
//                    match.getHomeDrawAwayOdds().get(1) + ", " + match.getFirstScorer() + ", " + (MATCH_NEXT_ID+1) + ")");

            statement.execute("INSERT INTO " + MatchTable.getTableName() + " (" + MatchTable.getColDate() + ", " +
                    MatchTable.getColHometeamId() + ", " + MatchTable.getColAwayteamId() + ", " + MatchTable.getColHomeXg() + ", " + MatchTable.getColAwayXg() + ", " +
                    MatchTable.getColHomeScore() + ", " + MatchTable.getColAwayScore() + ", " + MatchTable.getColHomeWinOdds() + ", " + MatchTable.getColAwayWinOdds() + ", " +
                    MatchTable.getColDrawOdds() + ", " + MatchTable.getColFirstScorer() + ", " + MatchTable.getColSofascoreId() + ", _id) " +
                        "VALUES ( '" + DateHelper.getSqlDate(match.getKickoffTime()) + "', " + homeTeamId + ", " + awayTeamId + ", " + match.getHomeXGF() + ", " + match.getAwayXGF() + ", " +
                        match.getHomeScore() + ", " + match.getAwayScore() + ", " + match.getHomeDrawAwayOdds().get(0) + ", " + match.getHomeDrawAwayOdds().get(2) + ", " +
                        match.getHomeDrawAwayOdds().get(1) + ", " + match.getFirstScorer() + ", " + match.getSofaScoreGameId() + ", " + ++MATCH_NEXT_ID + ")");


            match.getHomePlayerRatings().values().forEach(rating -> {
                writePlayerRatingsToDb(statement, rating, MATCH_NEXT_ID, homeTeamId);
            });
            match.getAwayPlayerRatings().values().forEach(rating -> {
                writePlayerRatingsToDb(statement, rating, MATCH_NEXT_ID, awayTeamId);
            });

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("ERROR writing match " + match.getHomeTeam().getTeamName() + " vs " + match.getAwayTeam().getTeamName() + " on " +  match.getKickoffTime() + " to db");
            e.printStackTrace();
        }
    }

    //TODO: can ABSOLUTELY use batch writes.
    private static void writePlayerRatingsToDb(Statement statement, PlayerRating playerRating, int matchId, int teamId) {
        try {

//            System.out.println("INSERT INTO " + PlayerRatingTable.getTableName() +
//                    " (" + PlayerRatingTable.getColPlayerName() + ", " + PlayerRatingTable.getColRating() + ", " + PlayerRatingTable.getColMins() + ", " +
//                    PlayerRatingTable.getColMatchId() + ", " + PlayerRatingTable.getColTeamId() + ", _id ) " +
//                    "VALUES ('" + playerRating.getName() + "', " + playerRating.getRating() + ", " + playerRating.getMinutesPlayed() + ", " +
//                        matchId + ", " + teamId + ", " + ++PLAYER_RATING_NEXT_ID + ")");

            statement.execute("INSERT INTO " + PlayerRatingTable.getTableName() +
                    " (" + PlayerRatingTable.getColPlayerName() + ", " + PlayerRatingTable.getColRating() + ", " + PlayerRatingTable.getColMins() + ", " +
                    PlayerRatingTable.getColMatchId() + ", " + PlayerRatingTable.getColTeamId() + ", _id ) " +
                    "VALUES ('" + playerRating.getName() + "', " + playerRating.getRating() + ", " + playerRating.getMinutesPlayed() + ", " +
                    matchId + ", " + teamId + ", " + ++PLAYER_RATING_NEXT_ID + ")");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("ERROR writing player " + playerRating.getName() + " from match id: " + matchId);
            e.printStackTrace();
        }
    }

    //TODO: CHECK IF WORKING!!!
    /*
     * Legacy code used to put the remaining matches of season in the database if the first write only put in a % of the matches.
     *
     * //TODO: MOVE RESULTSETS INTO TRY WITH RESOURCES SO IT AUTO CLOSES
     */
    public static void addRemainingMatchesOfSeason(League league, Season season, int seasonid) {

        season.getAllMatches().forEach(match -> {

            try (Statement statement = connection.createStatement()){

                String HOMETEAM = "hometeam";
                String AWAYTEAM = "awayteam";

//                System.out.println("SELECT count(*) FROM " + MatchTable.getTableName() +
//                        " INNER JOIN " + TeamTable.getTableName() + " AS " + HOMETEAM + " ON " + MatchTable.getColHometeamId() + " = " + HOMETEAM + "._id" +
//                        " INNER JOIN " + TeamTable.getTableName() + " AS " + AWAYTEAM + " ON " + MatchTable.getColAwayteamId() + " = " + AWAYTEAM + "._id" +
//                        " INNER JOIN " + SeasonTable.getTableName() + " ON " + TeamTable.getColSeasonId() + " = " + SeasonTable.getTableName() + "._id" +
//                        " INNER JOIN " + LeagueTable.getTableName() + " ON " + SeasonTable.getColLeagueId() + " = " + LeagueTable.getTableName() + "._id" +
//                        " WHERE " + LeagueTable.getTableName() + "." + LeagueTable.getColName() + " = '" + league.getName() + "'" +
//                        " AND " + SeasonTable.getTableName() + "." + SeasonTable.getColYearBeginning() + " = '" + season.getSeasonKey() + "'" +
//                        " AND " + HOMETEAM + "." + TeamTable.getColTeamName() + " = '" + match.getHomeTeam().getTeamName() + "'" +
//                        " AND " + AWAYTEAM + "." + TeamTable.getColTeamName() + " = '" + match.getAwayTeam().getTeamName() + "'");

                ResultSet result = statement.executeQuery("SELECT count(*) FROM " + MatchTable.getTableName() +
                        " INNER JOIN " + TeamTable.getTableName() + " AS " + HOMETEAM + " ON " + MatchTable.getColHometeamId() + " = " + HOMETEAM + "._id" +
                        " INNER JOIN " + TeamTable.getTableName() + " AS " + AWAYTEAM + " ON " + MatchTable.getColAwayteamId() + " = " + AWAYTEAM + "._id" +
                        " INNER JOIN " + SeasonTable.getTableName() + " ON " + HOMETEAM + "." + TeamTable.getColSeasonId() + " = " + SeasonTable.getTableName() + "._id" +
                        " INNER JOIN " + LeagueTable.getTableName() + " ON " + SeasonTable.getColLeagueId() + " = " + LeagueTable.getTableName() + "._id" +
                        " WHERE " + LeagueTable.getTableName() + "." + LeagueTable.getColName() + " = '" + league.getName() + "'" +
                        " AND " + SeasonTable.getTableName() + "." + SeasonTable.getColYearBeginning() + " = '" + season.getSeasonKey() + "'" +
                        " AND " + HOMETEAM + "." + TeamTable.getColTeamName() + " = '" + match.getHomeTeam().getTeamName() + "'" +
                        " AND " + AWAYTEAM + "." + TeamTable.getColTeamName() + " = '" + match.getAwayTeam().getTeamName() + "'");

                boolean found = result.getInt(1) == 1;

                if (!found) {
                   writeMatchToDb(statement, match, 2);
                }

            } catch (SQLException e) {
                System.out.println(e.getMessage());
                System.out.println("ERROR writing match " + match.getHomeTeam().getTeamName() + " vs " +match.getAwayTeam().getTeamName() + "on" + match.getKickoffTime());
                e.printStackTrace();
            }

        });
    }


    //Code that can be quickly copied into a sql command
//    SELECT player_ratings.player_name, player_ratings.mins_played,
//    player_ratings.rating, playsFor.team_name AS 'players_team', match.date, h.team_name AS 'hometeam_name', match.home_score,
//    match.home_xG, a.team_name AS 'awayteam_name', match.away_score, match.away_xG,
//    match.home_win_odds, match.draw_odds, match.away_win_odds,
//    match.first_scorer, season.season_years, league.name FROM player_ratings
//    INNER JOIN match ON player_ratings.match = match._id
//    INNER JOIN team as playsFor ON player_ratings.team = playsFor._id
//    INNER JOIN team AS h ON match.hometeam_id = h._id
//    INNER JOIN team AS a ON match.awayteam_id = a._id
//    INNER JOIN season ON h.season_id = season._id
//    INNER JOIN league ON season.league_id = league._id
//    WHERE league.name = 'EPL'
//    ORDER BY date;

        //to delete all player ratings from a certain league
//    DELETE FROM player_ratings
//    WHERE player_ratings.match IN (
//            SELECT match._id from match
//            INNER JOIN team ON (match.awayteam_id = team._id)
//    INNER JOIN season ON (team.season_id = season._id)
//    INNER JOIN league ON (season.league_id = league._id)
//    WHERE league.name = 'LIGUE_1'
//            );
//

//        DELETE FROM match
//    WHERE match.awayteam_id IN (
//            SELECT team._id from team
//            INNER JOIN season ON (team.season_id = season._id)
//    INNER JOIN league ON (season.league_id = league._id)
//    WHERE league.name = 'LIGUE_1'
//            );



    /*
     * Method to be called by machineLearning.GetMatchesFromDb and will return a ResultSet of all Player Ratings populated with
     * Teams, Matches, leagues.
     * From here we can calculate our training data and save into it's own database or file so we can recall whenever we want.
     */
    public static ArrayList getLeagueData(LeagueSeasonIds leagueSeasonIds) {
        try {

//sql
//                SELECT player_ratings.player_name, player_ratings.mins_played,
//    player_ratings.rating, playsFor.team_name AS 'players_team', match.date, h.team_name AS 'hometeam_name', match.home_score,
//    match.home_xG, a.team_name AS 'awayteam_name', match.away_score, match.away_xG,
//    match.home_win_odds, match.draw_odds, match.away_win_odds,
//    match.first_scorer, match._id, season.season_years, league.name FROM player_ratings
//    INNER JOIN match ON player_ratings.match = match._id
//    INNER JOIN team as playsFor ON player_ratings.team = playsFor._id
//    INNER JOIN team AS h ON match.hometeam_id = h._id
//    INNER JOIN team AS a ON match.awayteam_id = a._id
//    INNER JOIN season ON h.season_id = season._id
//    INNER JOIN league ON season.league_id = league._id
//    WHERE league.name = 'EPL'
//    ORDER BY date;

            Statement statement = connection.createStatement();

            String PLAYERS_TEAM = "players_team";
            String HOMETEAM = "hometeam";
            String AWAYTEAM = "awayteam";

            //gets all data we need to plug back into our classes for every player rating, sorted firstly by date, and then by the match id, then the team that the player
            //played for and then finally by how many minutes the player played. This gives us grouped player ratings by match and team, ordered by minutes played.
            ResultSet playerRatingsRows = statement.executeQuery("SELECT " + PlayerRatingTable.getTableName() + "." + PlayerRatingTable.getColPlayerName() + ", " +
                    PlayerRatingTable.getTableName() + "." + PlayerRatingTable.getColMins() + ", " + PlayerRatingTable.getTableName() + "." + PlayerRatingTable.getColRating() + ", " +
                    PLAYERS_TEAM + "." + TeamTable.getColTeamName() + " AS '" + PLAYERS_TEAM + "', " + MatchTable.getTableName() + "." + MatchTable.getColDate() + ", " +
                    HOMETEAM + "." + TeamTable.getColTeamName() + " AS '" + HOMETEAM + "', " + MatchTable.getTableName() + "." + MatchTable.getColHomeScore() + ", " +
                    MatchTable.getTableName() + "." + MatchTable.getColHomeXg() + ", " + AWAYTEAM + "." + TeamTable.getColTeamName() + " AS '" + AWAYTEAM + "', " +
                    MatchTable.getTableName() + "." + MatchTable.getColAwayScore() + ", " + MatchTable.getTableName() + "." + MatchTable.getColAwayXg() + ", " +
                    MatchTable.getTableName() + "." + MatchTable.getColHomeWinOdds() + ", " + MatchTable.getTableName() + "." + MatchTable.getColDrawOdds() + ", " +
                    MatchTable.getTableName() + "." + MatchTable.getColAwayWinOdds() + ", " + MatchTable.getTableName() + "." + MatchTable.getColFirstScorer() + ", " +
                    MatchTable.getTableName() + "._id, " + SeasonTable.getTableName() + "." + SeasonTable.getColYearBeginning() + ", " + LeagueTable.getTableName() + "." + LeagueTable.getColName() +
                    " FROM " + PlayerRatingTable.getTableName() +
                    " INNER JOIN " + MatchTable.getTableName() + " ON " + PlayerRatingTable.getTableName() + "." + PlayerRatingTable.getColMatchId() + " = " + MatchTable.getTableName() + "._id" +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + PLAYERS_TEAM + " ON " + PlayerRatingTable.getTableName() + "." + PlayerRatingTable.getColTeamId() + " = " + PLAYERS_TEAM + "._id" +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + HOMETEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColHometeamId() + " = " + HOMETEAM + "._id" +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + AWAYTEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColAwayteamId() + " = " + AWAYTEAM + "._id" +
                    " INNER JOIN " + SeasonTable.getTableName() + " ON " + HOMETEAM + "." + TeamTable.getColSeasonId() + " = " + SeasonTable.getTableName() + "._id" +
                    " INNER JOIN " + LeagueTable.getTableName() + " ON " + SeasonTable.getTableName() + "." + SeasonTable.getColLeagueId() + " = " + LeagueTable.getTableName() + "._id" +
                    " WHERE " + LeagueTable.getTableName() + "." + LeagueTable.getColName() + " = '" + leagueSeasonIds.name() + "'" +
                    " ORDER BY " + MatchTable.getTableName() + "." + MatchTable.getColDate() + ", " + MatchTable.getTableName() + "._id, " + PLAYERS_TEAM + ", " + PlayerRatingTable.getTableName() + "." + PlayerRatingTable.getColMins() + " DESC");

            ArrayList statementAndResults = new ArrayList(); //no type arraylist as we are passing both the statement and resultset to another function.
            statementAndResults.add(statement);
            statementAndResults.add(playerRatingsRows);

            return statementAndResults;

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /*
     * Used to decide where we need to start scraping in new games from.
     */
    public static String getMostRecentMatchInDb(LeagueSeasonIds leagueSeasonIds) {
        try (Statement statement = connection.createStatement()) {

//            SELECT match.date FROM match
//            INNER JOIN team ON match.hometeam_id = team._id
//            INNER JOIN season ON team.season_id = season._id
//            INNER JOIN league ON season.league_id = league._id
//            WHERE league.name = 'EPL'
//              AND match.home_score > -1
//            ORDER BY date DESC
//            LIMIT 1;

            ResultSet dateStringRS = statement.executeQuery("SELECT " + MatchTable.getTableName() + "." + MatchTable.getColDate() + " FROM " + MatchTable.getTableName() +
                    " INNER JOIN " + TeamTable.getTableName() + " ON " + MatchTable.getTableName() + "." + MatchTable.getColHometeamId() + " = " + TeamTable.getTableName() + "._id" +
                    " INNER JOIN " + SeasonTable.getTableName() + " ON " + TeamTable.getTableName() + "." + TeamTable.getColSeasonId() + " = " + SeasonTable.getTableName() + "._id" +
                    " INNER JOIN " + LeagueTable.getTableName() + " ON " + SeasonTable.getTableName() + "." + SeasonTable.getColLeagueId() + " = " + LeagueTable.getTableName() + "._id" +
                    " WHERE " + LeagueTable.getTableName() + "." + LeagueTable.getColName() + " = '" + leagueSeasonIds.name() + "'" +
                    " AND " + MatchTable.getTableName() + "." + MatchTable.getColHomeScore() + " > " + "-1" +
                    " ORDER BY " + MatchTable.getTableName() + "." + MatchTable.getColDate() + " DESC" +
                    " LIMIT " + "1");

            String dateString = dateStringRS.getString(1);


            return dateString;

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /*
     * Method will first find the match, and update it in the database. Then will add player ratings for that match.
     * Needs 2 db interactions per game
     *
     * TODO: This method along with other write-to-db methods need updating to use BulkWrite as this will be much faster.
     *
     * TODO: Could potentially update this method to just use Sofascore ID, that way we only need to interact with DB twice instead of 3 times (using UPDATE).
     *
     * TODO: BENEATH WAS ORIGINALLY WRITTEN ON LEAGUE.SCRAPEPLAYEDGAMES().
     * TODO: Will need to have an additional check when writing to database that the games have data in them. We have a problem at the moment where because
     * TODO of the discrepancies between the 2 websites on the kickoff time, we scrape in the shell of the game from Understat but then don't add in anything from
     * TODO sofascore because their date is on the date given.
     *
     * Method will check to see if the game already has data in the database (this will occasionally happen due to date differences between understat and sofascore websites)
     * If we already have data in the database, we do not update and we do not add player ratings (or we'd have duplicates).
     */
    public static void addPlayedGamesToDB(Season season) {
        try (Statement statement = connection.createStatement()) {

            getNextIds(); //gets the highest ID value so we can add in player ratings with continuous ID's

//            SELECT match._id AS 'match id', home._id AS 'home id', away._id AS 'away id' FROM match
//            INNER JOIN team AS home ON match.hometeam_id = home._id
//            INNER JOIN team AS away ON match.awayteam_id = away._id
//            INNER JOIN season ON home.season_id = season._id
//            WHERE home.team_name = 'Leicester'
//            AND away.team_name = 'Burnley'
//            AND season.season_years = '18-19'

            String seasonKey = season.getSeasonKey();
            int numbMatchesUpdated = 0;

            for (Match match: season.getAllMatches()) {
                String homeTeamName = match.getHomeTeam().getTeamName();
                String awayTeamName = match.getAwayTeam().getTeamName();

                ResultSet resultSet = statement.executeQuery("SELECT " + MatchTable.getTableName() + "._id, " + HOMETEAM + "._id, " + AWAYTEAM + "._id, " +
                        MatchTable.getTableName() + "." + MatchTable.getColHomeScore() + ", " + MatchTable.getTableName() + "." + MatchTable.getColHomeWinOdds() +
                        " FROM " + MatchTable.getTableName() +
                        " INNER JOIN " + TeamTable.getTableName() + " AS " + HOMETEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColHometeamId() + " = " + HOMETEAM + "._id" +
                        " INNER JOIN " + TeamTable.getTableName() + " AS " + AWAYTEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColAwayteamId() + " = " + AWAYTEAM + "._id" +
                        " INNER JOIN " + SeasonTable.getTableName() + " ON " + HOMETEAM + "." + TeamTable.getColSeasonId() + " = " + SeasonTable.getTableName() + "._id" +
                        " WHERE " + HOMETEAM + "." + TeamTable.getColTeamName() + " = '" + homeTeamName + "'" +
                        " AND " + AWAYTEAM + "." + TeamTable.getColTeamName() + " = '" + awayTeamName + "'" +
                        " AND " + SeasonTable.getTableName() + "." + SeasonTable.getColYearBeginning() + " = '" + seasonKey + "'");


                    //to write player ratings, we need the match id and the players team id.
                    int matchId = resultSet.getInt(1);
                    int hometeamId = resultSet.getInt(2);
                    int awayteamId = resultSet.getInt(3);
                    int homeScoreInDb = resultSet.getInt(4);
                    double homeWinOdds = resultSet.getDouble(5);


                    //needs to update homeXG, homescore, homeWinOdds, awayXG, awayscore, awaywinodds, drawodds, firstscorer, date (if a postponement has not been updated).
                    double homeXGF = match.getHomeXGF();
                    double awayXGF = match.getAwayXGF();
                    int homeScore = match.getHomeScore();
                    int awayScore = match.getAwayScore();
                    double homeOdds = match.getHomeOdds();
                    double drawOdds = match.getDrawOdds();
                    double awayOdds = match.getAwayOdds();
                    int firstScorer = match.getFirstScorer();
                    String kickOffTime = DateHelper.getSqlDate(match.getKickoffTime());

                    if (homeXGF == -1d || awayXGF == -1d || homeScore == -1 || awayScore == -1 || homeOdds == -1d || drawOdds == -1d || awayOdds == -1d) {
                        System.out.println("TRYING TO STORE A PLAYED MATCH TO DATABASE WITHOUT HAVING ALL REQUIRED INFO. " + homeTeamName + " vs " + awayTeamName + " on " + match.getKickoffTime());
                        System.out.println(homeXGF + "|" + awayXGF + "|" + homeScore + "|" + awayScore + "|" + homeOdds + "|" + drawOdds + "|" + awayOdds + "|" + firstScorer);

                        if (homeXGF > -1 && awayXGF > -1 && homeScore > -1 && awayScore > -1) {
                            System.out.println("Added the game to database anyway without betting data.");
                        } else {
                            continue;
                        }
                    }

                    //updating match
                    statement.execute("UPDATE " + MatchTable.getTableName() +
                            " SET " + MatchTable.getColHomeXg() + " = " + homeXGF + ", " + MatchTable.getColAwayXg() + " = " + awayXGF + ", " +
                            MatchTable.getColHomeScore() + " = " + homeScore + ", " + MatchTable.getColAwayScore() + " = " + awayScore + ", " +
                            MatchTable.getColHomeWinOdds() + " = " + homeOdds + ", " + MatchTable.getColDrawOdds() + " = " + drawOdds + ", " +
                            MatchTable.getColAwayWinOdds() + " = " + awayOdds + ", " + MatchTable.getColFirstScorer() + " = " + firstScorer + ", " +
                            MatchTable.getColDate() + " = '" + kickOffTime + "'" +
                            " WHERE " + MatchTable.getTableName() + "._id" + " = " + matchId);

                    //writing player ratings
                    HashMap<String, PlayerRating> homeRatings = match.getHomePlayerRatings();
                    HashMap<String, PlayerRating> awayRatings = match.getAwayPlayerRatings();

                    if (homeRatings.size() < 11 || awayRatings.size() < 11)
                        throw new RuntimeException("We have managed to scrape a game without all player ratings present. " + homeTeamName + " vs " + awayTeamName + " in " + seasonKey);

                    boolean homeTeamSuccessfulInsert = insertTeamsPlayerRatings(homeRatings, matchId, hometeamId, statement);
                    boolean awayTeamSuccessfulInsert = insertTeamsPlayerRatings(awayRatings, matchId, awayteamId, statement);

                    if (!homeTeamSuccessfulInsert && !awayTeamSuccessfulInsert) {
                        System.out.println("Both teams already have player ratings in DB " + homeTeamName + " vs " + awayTeamName + " on " + match.getKickoffTime());
                    } else if (!homeTeamSuccessfulInsert) {
                        System.out.println("Home team already has player ratings " + homeTeamName + " vs " + awayTeamName + " on " + match.getKickoffTime());
                    } else if (!awayTeamSuccessfulInsert) {
                        System.out.println("Away team already has player ratings " + homeTeamName + " vs " + awayTeamName + " on " + match.getKickoffTime());
                    }

                    numbMatchesUpdated++;

            }


            System.out.println("Updated " + numbMatchesUpdated + " into database");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            System.out.println("It's possible that our query to the database didn't return a result and therefore the resultset closes automatically, throwing this exception.");
            e.printStackTrace();
        }
    }

    private static boolean insertTeamsPlayerRatings(HashMap<String, PlayerRating> playerRatings, int matchId, int teamId, Statement statement) {
        StringBuilder SQL_INSERT_STATEMENT = createSqlInsertStatement();
        for (PlayerRating rating: playerRatings.values()) {
            addPlayerRatingsToStringBuilder(SQL_INSERT_STATEMENT, rating, matchId, teamId, ++PLAYER_RATING_NEXT_ID);
        }

        try {
            statement.execute(SQL_INSERT_STATEMENT.substring(0, SQL_INSERT_STATEMENT.length() - 2));
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private static StringBuilder createSqlInsertStatement() {
        return new StringBuilder("INSERT INTO " + PlayerRatingTable.getTableName() + " (" + PlayerRatingTable.getColPlayerName() + ", " +
                PlayerRatingTable.getColRating() + ", " + PlayerRatingTable.getColMins() + ", " + PlayerRatingTable.getColMatchId() + ", " +
                PlayerRatingTable.getColTeamId() + ", _id ) " + "VALUES ");
    }

    private static void addPlayerRatingsToStringBuilder(StringBuilder stringBuilder, PlayerRating playerRating, int matchId, int teamId, int playerId) {
        stringBuilder.append("( '");
        stringBuilder.append(playerRating.getName());
        stringBuilder.append("', ");
        stringBuilder.append(playerRating.getRating());
        stringBuilder.append(", ");
        stringBuilder.append(playerRating.getMinutesPlayed());
        stringBuilder.append(", ");
        stringBuilder.append(matchId);
        stringBuilder.append(", ");
        stringBuilder.append(teamId);
        stringBuilder.append(", ");
        stringBuilder.append(playerId);
        stringBuilder.append(" ), ");
    }

    /*
     * Needed because understat (who we created our dates from) sometimes have incorrect start dates and times.
     *
     * Called from SofaScore file.
     *
     * Will also be used to change postponed matches in the database.
     *
     * //TODO: needs a check to make sure the correct startDate format is given by the user.
     */
    public static void updateKickoffTime(String seasonYear, String homeTeamName, String awayTeamName, String startDate, int sofaScoreID) {
        try (Statement statement = connection.createStatement()) {

            System.out.println(homeTeamName + " vs " + awayTeamName + " on " + startDate);

            homeTeamName = Team.makeTeamNamesCompatible(homeTeamName);
            awayTeamName = Team.makeTeamNamesCompatible(awayTeamName);

            String HOMETEAM = "hometeam";
            String AWAYTEAM = "awayteam";
            //unfortunately cannot use an inner join with an update query, so must first get the team id's out of the database, then we can update.
            ResultSet resultSet = statement.executeQuery(
                    "SELECT " + HOMETEAM + "._id, " + AWAYTEAM + "._id, " + MatchTable.getTableName() + "." + MatchTable.getColDate() +
                            ", " + MatchTable.getTableName() + "." + MatchTable.getColSofascoreId() + " FROM " + MatchTable.getTableName() +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + HOMETEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColHometeamId() + " = " + HOMETEAM + "._id" +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + AWAYTEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColAwayteamId() + " = " + AWAYTEAM + "._id" +
                    " INNER JOIN " + SeasonTable.getTableName() + " ON " + HOMETEAM + "." + TeamTable.getColSeasonId() + " = " + SeasonTable.getTableName() + "._id " +
                    " WHERE " + SeasonTable.getTableName() + "." + SeasonTable.getColYearBeginning() + " = '" + seasonYear + "'" +
                    " AND " + HOMETEAM + "." + TeamTable.getColTeamName() + " = '" + homeTeamName + "'" +
                    " AND " + AWAYTEAM + "." + TeamTable.getColTeamName() + " = '" + awayTeamName + "'");

            if (resultSet.next()) { //setting resultSet pointer to first record.
                int homeTeamId = resultSet.getInt(1);
                int awayTeamId = resultSet.getInt(2);
                String dbDateString = resultSet.getString(3);
                int dbSofaScoreId = resultSet.getInt(4);

                if (!startDate.equals(dbDateString) || sofaScoreID != dbSofaScoreId) {

                    statement.execute("UPDATE " + MatchTable.getTableName() +
                            " SET " + MatchTable.getColDate() + " = '" + startDate + "', " + MatchTable.getColSofascoreId() + " = " + sofaScoreID +
                            " WHERE " + MatchTable.getColHometeamId() + " = " + homeTeamId +
                            " AND " + MatchTable.getColAwayteamId() + " = " + awayTeamId);

                }
            } else {
                System.out.println("We couldn't get a record from " + homeTeamName + " vs " + awayTeamName + " on " + startDate);
            }


        }
        catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /*
     * Gets the matches in the database that take place between 2 dates.
     * TODO: Possible problem if we have a match in there that has the wrong kickoff date and is actually a few days further ahead? Should we only get games with a sofascore Id?
     */
    public static ArrayList<MatchToPredict> getBaseMatchesToPredict(java.util.Date earliestKickoff, java.util.Date latestKickoff) {
//        SELECT h.team_name, a.team_name, season.season_years, league.name, match.sofascore_id, match.date FROM match
//        INNER JOIN team AS h ON match.hometeam_id = h._id
//        INNER JOIN team AS a ON match.awayteam_id = a._id
//        INNER JOIN season ON h.season_id = season._id
//        INNER JOIN league ON season.league_id = league._id
//        WHERE match.date > "2018-12-15 00:00:00"
//        AND match.date < "2018-12-15 23:59:00"
//        AND match.sofaScore_id != -1;
        ArrayList<MatchToPredict> matches = new ArrayList<>();

        try (Statement statement = connection.createStatement()) {

            String HOMETEAM = "hometeam";
            String AWAYTEAM = "awayteam";

            String earliestDate = DateHelper.getSqlDate(earliestKickoff);
            String latestDate = DateHelper.getSqlDate(latestKickoff);

            ResultSet resultSet = statement.executeQuery("SELECT " + HOMETEAM + "." + TeamTable.getColTeamName() + ", " + AWAYTEAM + "." + TeamTable.getColTeamName() +
                    ", " + SeasonTable.getTableName() + "." + SeasonTable.getColYearBeginning() + ", " + LeagueTable.getTableName() + "." + LeagueTable.getColName() + ", " +
                    MatchTable.getTableName() + "." + MatchTable.getColSofascoreId() + ", " + MatchTable.getTableName() + "." + MatchTable.getColDate() + ", " +
                    MatchTable.getTableName() + "._id" +
                    " FROM " + MatchTable.getTableName() +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + HOMETEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColHometeamId() + " = " + HOMETEAM + "._id" +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + AWAYTEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColAwayteamId() + " = " + AWAYTEAM + "._id" +
                    " INNER JOIN " + SeasonTable.getTableName() + " ON " + HOMETEAM + "." + TeamTable.getColSeasonId() + " = " + SeasonTable.getTableName() + "._id" +
                    " INNER JOIN " + LeagueTable.getTableName() + " ON " + SeasonTable.getTableName() + "." + SeasonTable.getColLeagueId() + " = " + LeagueTable.getTableName() + "._id" +
                    " WHERE " + MatchTable.getTableName() + "." + MatchTable.getColDate() + " >= '" + earliestDate + "'" +
                    " AND " + MatchTable.getTableName() + "." + MatchTable.getColDate() + " <= '" + latestDate + "'" +
                    " AND " + MatchTable.getTableName() + "." + MatchTable.getColSofascoreId() + " != -1");

            while (resultSet.next()) {

                String homeTeam = resultSet.getString(1);
                String awayTeam = resultSet.getString(2);
                String seasonKey = resultSet.getString(3);
                String leagueName = resultSet.getString(4);
                int sofaScoreId = resultSet.getInt(5);
                String kickOffTime = resultSet.getString(6);
                int database_id = resultSet.getInt(7);

                //String homeTeamName, String awayTeamName, String seasonKey, String understatUrl, int sofaScoreId
                MatchToPredict match = new MatchToPredict(homeTeam, awayTeam, seasonKey, leagueName, sofaScoreId, kickOffTime, database_id);

                matches.add(match);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return matches;
    }

    /*
     * homeDrawAway is int and 0 is home win, 1 is draw, 2 is away win.
     * TODO: refactor to just use the database Id stored in the matchtopredict. Also change these homeDrawAway value to use enums to avoid errors and ambiguity
     */
    public static void legacyLogBetPlaced(String homeTeamName, String awayTeamName, String seasonKey, int homeDrawAway, double oddsAtTimeOfBet, double stake) {
        //we first need to get the ids of home and away teams because sqlite doesn't support joins on updates.
        try (Statement statement = connection.createStatement()) {

            java.util.Date beginningOfToday = DateHelper.setTimeOfDate(new java.util.Date(), 0, 0, 0);
            java.util.Date endOfToday = DateHelper.setTimeOfDate(new java.util.Date(), 23, 59, 59);

            String HOMETEAM = "hometeam";
            String AWAYTEAM = "awayteam";

            ResultSet resultSet = statement.executeQuery(
                    "SELECT " + HOMETEAM + "._id, " + AWAYTEAM + "._id, " + MatchTable.getTableName() + "." + MatchTable.getColDate() +
                            ", " + MatchTable.getTableName() + "." + MatchTable.getColSofascoreId() + " FROM " + MatchTable.getTableName() +
                            " INNER JOIN " + TeamTable.getTableName() + " AS " + HOMETEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColHometeamId() + " = " + HOMETEAM + "._id" +
                            " INNER JOIN " + TeamTable.getTableName() + " AS " + AWAYTEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColAwayteamId() + " = " + AWAYTEAM + "._id" +
                            " INNER JOIN " + SeasonTable.getTableName() + " ON " + HOMETEAM + "." + TeamTable.getColSeasonId() + " = " + SeasonTable.getTableName() + "._id " +
                            " WHERE " + SeasonTable.getTableName() + "." + SeasonTable.getColYearBeginning() + " = '" + seasonKey + "'" +
                            " AND " + HOMETEAM + "." + TeamTable.getColTeamName() + " = '" + homeTeamName + "'" +
                            " AND " + AWAYTEAM + "." + TeamTable.getColTeamName() + " = '" + awayTeamName + "'");

            while (resultSet.next()) {

                int homeTeamId = resultSet.getInt(1);
                int awayTeamId = resultSet.getInt(2);
                String kickoff = resultSet.getString(3);

                java.util.Date kickOffDate = DateHelper.createDateFromSQL(kickoff);
                if (!beginningOfToday.before(kickOffDate) || !endOfToday.after(kickOffDate)) {
                    throw new RuntimeException("Date in database is on a different date to that in the database. Match between " + homeTeamName + " and " + awayTeamName +
                            " is schedulled for " + kickOffDate + " in the database.");
                } else {

                    statement.execute("UPDATE " + MatchTable.getTableName() +
                            " SET " + MatchTable.getColResultBetOn() + " = " + homeDrawAway + ", " + MatchTable.getColOddsWhenBetPlaced() + " = " + oddsAtTimeOfBet +
                            MatchTable.getColStakeOnBet() + " = " + stake +
                            " WHERE " + MatchTable.getColHometeamId() + " = " + homeTeamId +
                            " AND " + MatchTable.getColAwayteamId() + " = " + awayTeamId);

                }


            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public static void logBetPlaced(MatchLog matchLog) {

        try (Statement statement = connection.createStatement()) {

            statement.execute("UPDATE " + MatchTable.getTableName() +
                    " SET " +
                    MatchTable.getColResultBetOn() + " = " + matchLog.getResultBetOn().getSqlIntCode() + ", " +
                    MatchTable.getColOddsWhenBetPlaced() + " = " + matchLog.getOddsBetOn() + ", " +
                    MatchTable.getColStakeOnBet() + " = " + matchLog.getStake() + ", " +
                    MatchTable.getColWhenGameWasPredicted() + " = " + matchLog.getWhenGameWasPredicted().getSqlIntCode() +
                    " WHERE _id = " + matchLog.getMatch().getDatabase_id());


        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * Method will return a set of games our database decided to bet on.
     * It will also total these bets up as it adds these and this result can be obtained by looking at the BetResultsTotalled class.
     */
    public static BetResultsTotalled getResultsOfPredictions(java.util.Date earliestMatchPredicted, java.util.Date latestMatchPredicted,
                                                             WhenGameWasPredicted whenGameWasPredicted) {
        HashSet<BetResult> betResults = new HashSet<>();
        BetResultsTotalled totalledResults = new BetResultsTotalled();

        String EARLIEST = "earliest";
        String LATEST = "latest";
        HashMap<String, String> dates = turnDatesToSqlCompatible(earliestMatchPredicted, latestMatchPredicted, EARLIEST, LATEST);

        String sqlEarliestMatch = dates.get(EARLIEST);
        String sqlLatestMatch = dates.get(LATEST);


        try (Statement statement = connection.createStatement()) {

            String HOMETEAM = "hometeam";
            String AWAYTEAM = "awayteam";

            String filter = getSqlFilterForWhenBetWasPlaced(whenGameWasPredicted);

            ResultSet resultSet = statement.executeQuery("SELECT " + HOMETEAM + "." + TeamTable.getColTeamName() + ", " + AWAYTEAM + "." + TeamTable.getColTeamName() + ", " +
                    MatchTable.getTableName() + "." + MatchTable.getColDate() + ", " + MatchTable.getTableName() + "." + MatchTable.getColStakeOnBet() + ", " +
                    MatchTable.getTableName() + "." + MatchTable.getColOddsWhenBetPlaced() + ", " + MatchTable.getTableName() + "." + MatchTable.getColResultBetOn() + ", " +
                    MatchTable.getTableName() + "." + MatchTable.getColHomeScore() + ", " + MatchTable.getTableName() + "." + MatchTable.getColAwayScore() +
                    " FROM " + MatchTable.getTableName() +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + HOMETEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColHometeamId() + " = " + HOMETEAM + "._id" +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + AWAYTEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColAwayteamId() + " = " + AWAYTEAM + "._id" +
                    " WHERE " + filter +
                    " AND " + MatchTable.getTableName() + "." + MatchTable.getColDate() + " >= '" + sqlEarliestMatch + "'" +
                    " AND " + MatchTable.getTableName() + "." + MatchTable.getColDate() + " <= '" + sqlLatestMatch + "'");

            while (resultSet.next()) {

                String homeTeamName = resultSet.getString(1);
                String awayTeamName = resultSet.getString(2);
                String dateString = resultSet.getString(3);
                double stakeOnBet = resultSet.getDouble(4);
                double oddsWhenBetPlaced = resultSet.getDouble(5);
                int resultBetOn = resultSet.getInt(6);
                int homeScore = resultSet.getInt(7);
                int awayScore = resultSet.getInt(8);
                int result = calculateResult(homeScore, awayScore);

                java.util.Date date = DateHelper.createDateFromSQL(dateString);

                BetResult betResult = new BetResult(date, homeTeamName, awayTeamName, stakeOnBet, oddsWhenBetPlaced, resultBetOn, result);
                betResults.add(betResult);

                //double moneyOut, double odds, int resultBetOn, int result
                totalledResults.addBet(stakeOnBet, oddsWhenBetPlaced, resultBetOn, result);
            }


        } catch (Exception e) {
            e.printStackTrace();
        }

        return totalledResults;
    }

    private static String getSqlFilterForWhenBetWasPlaced(WhenGameWasPredicted whenGameWasPredicted) {
        StringBuilder filterForWhenBetWasPlaced = new StringBuilder();

        filterForWhenBetWasPlaced.append(MatchTable.getTableName());
        filterForWhenBetWasPlaced.append(".");
        filterForWhenBetWasPlaced.append(MatchTable.getColWhenGameWasPredicted());

        if (whenGameWasPredicted == null){
            filterForWhenBetWasPlaced.append(" != ");
            filterForWhenBetWasPlaced.append(WhenGameWasPredicted.NOT_PREDICTED_ON.getSqlIntCode());
        } else {
            filterForWhenBetWasPlaced.append(" = ");
            filterForWhenBetWasPlaced.append(whenGameWasPredicted.getSqlIntCode());
        }

        filterForWhenBetWasPlaced.append(" ");
        return filterForWhenBetWasPlaced.toString();
    }

    private static HashMap<String, String> turnDatesToSqlCompatible (java.util.Date earliestMatchPredicted, java.util.Date latestMatchPredicted, String earliestKey, String latestKey) {
        HashMap<String, String> dates = new HashMap<>();

        if (earliestMatchPredicted == null) {
            dates.put(earliestKey, "1970-01-01 00:00:00");
        } else {
            dates.put(earliestKey, DateHelper.getSqlDate(earliestMatchPredicted));
        }

        if (latestMatchPredicted == null) {
            dates.put(latestKey, DateHelper.getSqlDate(new java.util.Date()));
        } else {
            dates.put(latestKey, DateHelper.getSqlDate(latestMatchPredicted));
        }

        return dates;
    }

    private static int calculateResult (int homeScore, int awayScore) {
        //TODO: refactor so we return enums instead of just base ints so we have consistency across files.
        if (homeScore > awayScore) return 0;
        else if (homeScore == awayScore) return 1;
        else return 2;
    }

    /*
     * Method will calculate the percentage of how many games our program was running for and able to make a prediction in real time on.
     * Starting from the date where we first implemented the option to predict games that we missed (in order to get a better picture of how the model was performing).
     */
    public static UptimeData getModelOnlinePercentage() {
        java.util.Date WHEN_CHANGED_DATABASE_TO_RECORD_PREDICTIONS = DateHelper.setDate(2019, 01, 17);
        String earliestDateString = DateHelper.getSqlDate(WHEN_CHANGED_DATABASE_TO_RECORD_PREDICTIONS);

        //make 2 calls to the database - one to count the number of games we predicted, and then another to count the number we missed but still predicted

        try (Statement statement1 = connection.createStatement();
             Statement statement2 = connection.createStatement();

             ResultSet realTime = statement1.executeQuery("SELECT count(*) FROM " + MatchTable.getTableName() +
                    " WHERE " + MatchTable.getColWhenGameWasPredicted() + " = " + WhenGameWasPredicted.PREDICTED_ON_IN_REAL_TIME.getSqlIntCode() +
                     " AND " + MatchTable.getColDate() + " >= '" + earliestDateString + "'");
             ResultSet predictedLaterOn = statement2.executeQuery("SELECT count(*) FROM " + MatchTable.getTableName() +
                     " WHERE " + MatchTable.getColWhenGameWasPredicted() + " = " + WhenGameWasPredicted.PREDICTED_LATER_ON.getSqlIntCode() +
                     " AND " + MatchTable.getColDate() + " >= '" + earliestDateString + "'");
        ) {

            realTime.next();
            predictedLaterOn.next();
            int numbPredictedRealTime = realTime.getInt(1);
            int numbPredictedLater = predictedLaterOn.getInt(1);

            return new UptimeData(numbPredictedLater, numbPredictedRealTime, DateHelper.turnDateToddMMyyyyString(WHEN_CHANGED_DATABASE_TO_RECORD_PREDICTIONS));

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    /*
     * Method will look to see firstly if the match is after when we last predicted our games. Then it will only get games that have results in there and haven't yet been
     * predicted on. There was a potential
     * problem with this in that we only scrape in results every 2 days, so if we predicted all missed games and stored when we last predicted missed games, there may be some
     * games that are later updated with results that were before the time when we last predicted missed games... i.e. there would be missed games that we'd again miss predicting.
     *
     * This has been solved in the logging.LastPredicted file where when we set when we last predicted missed games, we write 2 days before the current date, to account for results
     * not all being there on the day.
     *
     * Stored as an Arraylist so we can maintain order and use that when we go through every stored match to check whether the date is after when we're supposed to be predicting
     * missed games. Will come back from the database sorted with the earliest date first.
     */
    public static ArrayList<MatchToPredict> getMatchesWithoutPredictions(java.util.Date sinceWhen) {

        ArrayList<MatchToPredict> matchesWithoutPredictions = new ArrayList<>();

        java.util.Date withoutHours = DateHelper.removeTimeFromDate(sinceWhen);
        String earliestMatchToLookAt = DateHelper.getSqlDate(withoutHours);


        try (Statement statement = connection.createStatement()) {

            String HOMETEAM = "hometeam";
            String AWAYTEAM = "awayteam";

            //we need homeTeamName, awayTeamName, seasonKey, leagueID, sofascoreId, dateString
            //also need odds for home draw away.
            //also will add in the _id from the database so it will be easy to update the records later
            ResultSet resultSet = statement.executeQuery("SELECT " + HOMETEAM + "." + TeamTable.getColTeamName() + ", " + AWAYTEAM + "." + TeamTable.getColTeamName() + ", " +
                    SeasonTable.getTableName() + "." + SeasonTable.getColYearBeginning() + ", " + LeagueTable.getTableName() + "." + LeagueTable.getColName() + ", " +
                    MatchTable.getTableName() + "." + MatchTable.getColSofascoreId() + ", " + MatchTable.getTableName() + "." + MatchTable.getColDate() + ", " +
                    MatchTable.getTableName() + "." + MatchTable.getColHomeWinOdds() + ", " + MatchTable.getTableName() + "." + MatchTable.getColDrawOdds() + ", " +
                    MatchTable.getTableName() + "." + MatchTable.getColAwayWinOdds() + ", " + MatchTable.getTableName() + "._id" +
                    " FROM " + MatchTable.getTableName() +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + HOMETEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColHometeamId() + " = " + HOMETEAM + "._id" +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + AWAYTEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColAwayteamId() + " = " + AWAYTEAM + "._id" +
                    " INNER JOIN " + SeasonTable.getTableName() + " ON " + HOMETEAM + "." + TeamTable.getColSeasonId() + " = " + SeasonTable.getTableName() + "._id" +
                    " INNER JOIN " + LeagueTable.getTableName() + " ON " + SeasonTable.getColLeagueId() + " = " + LeagueTable.getTableName() + "._id" +
                    " WHERE " + MatchTable.getTableName() + "." + MatchTable.getColDate() + " >= '" + earliestMatchToLookAt + "'" +
                    " AND " + MatchTable.getTableName() + "." + MatchTable.getColHomeScore() + " != -1" +
                    " AND " + MatchTable.getTableName() + "." + MatchTable.getColWhenGameWasPredicted() + " = -1" +
                    " ORDER BY " + MatchTable.getTableName() + "." + MatchTable.getColDate() + " ASC");

            while (resultSet.next()) {

                String homeTeamName = resultSet.getString(1);
                String awayTeamName = resultSet.getString(2);
                String seasonKey = resultSet.getString(3);
                String leagueName = resultSet.getString(4);
                int sofaScoreId = resultSet.getInt(5);
                String sqlDateString = resultSet.getString(6);
                double homeWinOdds = resultSet.getDouble(7);
                double drawOdds = resultSet.getDouble(8);
                double awayWinOdds = resultSet.getDouble(9);
                int idFromDatabase = resultSet.getInt(10);

                MatchToPredict match = new MatchToPredict(homeTeamName, awayTeamName, seasonKey, leagueName, sofaScoreId, sqlDateString, idFromDatabase);

                double[] odds = new double[]{homeWinOdds, drawOdds, awayWinOdds};
                LinkedHashMap<String, double[]> allBookiesOdds = new LinkedHashMap<>();
                allBookiesOdds.put(OddsCheckerBookies.BET365.getBookie(), odds); //bet365 as these are what are on the sofascore website and the only odds we have stored in database.
                match.setBookiesOdds(allBookiesOdds);

                matchesWithoutPredictions.add(match);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return matchesWithoutPredictions;
    }


    /*
     * Method to be used to add data to games that do not have data in the database. can be caused by sites we scrape from not having all the data when we scrape.
     * Selects games without score data or bet data
     *
     * Adds matches and also creates teams. Then adds the created matches to the teams as this is how matches will mostly be accessed.
     *
     */
    public static void addMissedMatches(HashSet<League> allLeagues) {

        try (Statement statement1 = connection.createStatement();
             Statement statement2 = connection.createStatement();
             Statement statement3 = connection.createStatement()) {

            ResultSet r1 = getMatchesWithoutScoreOrOdds(statement1);
            ResultSet r2 = getMatchesWhereOneTeamDoesNotHavePlayerRatings(statement2);
            ResultSet r3 = getMatchesWithoutBothTeamsHavingPlayerRatings(statement3);

            ResultSet[] matchesToAdd = new ResultSet[]{r1, r2, r3};

            League currLeague = null;
            Season currSeason = null;
            for (ResultSet resultSet : matchesToAdd) {
                while (resultSet.next()) {
                    String homeTeamName = resultSet.getString(1);
                    String awayTeamName = resultSet.getString(2);
                    String date = resultSet.getString(3);
                    String seasonYear = resultSet.getString(4);
                    String leagueName = resultSet.getString(5);
                    if (homeTeamName.equals("SC Bastia") && awayTeamName.equals("Lyon") && seasonYear.equals("16-17"))
                        continue; //sofascore does not have player ratings for this game.

                    if (currLeague == null || !currLeague.getName().equals(leagueName)) {
                        currLeague = allLeagues.stream()
                                .filter(l -> l.getName().equals(leagueName))
                                .findFirst()
                                .orElseThrow(RuntimeException::new);
                        currSeason = currLeague.getSeason(seasonYear);
                    } else if (currSeason == null || !currSeason.getSeasonKey().equals(seasonYear)) {
                        currSeason = currLeague.getSeason(seasonYear);
                    }

                    //try to find teams
                    Team homeTeam = currSeason.getTeam(homeTeamName);
                    if (homeTeam == null) {
                        homeTeam = new Team(homeTeamName);
                        currSeason.addNewTeam(homeTeam);
                    }

                    Team awayTeam = currSeason.getTeam(awayTeamName);
                    if (awayTeam == null) {
                        awayTeam = new Team(awayTeamName);
                        currSeason.addNewTeam(awayTeam);
                    }

                    java.util.Date kickoffTime = DateHelper.createDateFromSQL(date);

                    Match match = homeTeam.getMatchFromAwayTeamName(awayTeamName);
                    if (match == null) {
                        match = new Match(homeTeam, awayTeam, kickoffTime);
                        homeTeam.addMatch(match);
                        awayTeam.addMatch(match);
                        currSeason.addNewMatch(match);
                    }
                }
            }

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }

    private static ResultSet getMatchesWithoutScoreOrOdds(Statement statement) {
        java.util.Date todayNoTime = DateHelper.setTimeOfDate(new java.util.Date(), 0, 0, 0);
        String sqlBeginningOfToday = DateHelper.getSqlDate(todayNoTime);

        try {
            return statement.executeQuery("SELECT " + HOMETEAM + "." + TeamTable.getColTeamName() + ", " + AWAYTEAM + "." + TeamTable.getColTeamName() + ", " +
                    MatchTable.getTableName() + "." + MatchTable.getColDate() + ", " + SeasonTable.getTableName() + "." + SeasonTable.getColYearBeginning() + ", " +
                    LeagueTable.getTableName() + "." + LeagueTable.getColName() +
                    " FROM " + MatchTable.getTableName() +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + HOMETEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColHometeamId() + " = " + HOMETEAM + "._id" +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + AWAYTEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColAwayteamId() + " = " + AWAYTEAM + "._id" +
                    " INNER JOIN " + SeasonTable.getTableName() + " ON " + HOMETEAM + "." + TeamTable.getColSeasonId() + " = " + SeasonTable.getTableName() + "._id" +
                    " INNER JOIN " + LeagueTable.getTableName() + " ON " + SeasonTable.getTableName() + "." + SeasonTable.getColLeagueId() + " = " + LeagueTable.getTableName() + "._id" +
                    " WHERE " + MatchTable.getTableName() + "." + MatchTable.getColDate() + " < '" + sqlBeginningOfToday + "'" +
                    " AND (" + MatchTable.getTableName() + "." + MatchTable.getColHomeScore() + " < " + 0 +
                    " OR " + MatchTable.getTableName() + "." + MatchTable.getColHomeWinOdds() + " < " + 0 + ")" +
                    " ORDER BY " + LeagueTable.getTableName() + "." + LeagueTable.getColName() + ", " + SeasonTable.getTableName() + "." + SeasonTable.getColYearBeginning() + " ASC");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    //TODO: see if we can just scrape in all new data without deleting the teams ratings that are already in there.
    //TODO: if we end up with duplicates, we will have to add some logic in here to make a delete request to the teams that have ratings in there. If we do this, we'll need to
    //todo: enforce getMatchesWithoutBothTeamsHavingPlayerRatings() method before this one so that we do not get repeated games.
    private static ResultSet getMatchesWhereOneTeamDoesNotHavePlayerRatings(Statement statement) {
        java.util.Date todayNoTime = DateHelper.setTimeOfDate(new java.util.Date(), 0, 0, 0);
        String sqlBeginningOfToday = DateHelper.getSqlDate(todayNoTime);

        String countOfPlayersInMatchCol = "playersInMatch";

        try {
            return statement.executeQuery("SELECT " + HOMETEAM + "." + TeamTable.getColTeamName() + ", " + AWAYTEAM + "." + TeamTable.getColTeamName() + ", " +
                    MatchTable.getTableName() + "." + MatchTable.getColDate() + ", " + SeasonTable.getTableName() + "." + SeasonTable.getColYearBeginning() + ", " +
                    LeagueTable.getTableName() + "." + LeagueTable.getColName() + ", " + "COUNT(*) AS " + countOfPlayersInMatchCol +
                    " FROM " + PlayerRatingTable.getTableName() +
                    " INNER JOIN " + MatchTable.getTableName() + " ON " + PlayerRatingTable.getTableName() + "." + PlayerRatingTable.getColMatchId() + " = " + MatchTable.getTableName() + "._id" +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + HOMETEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColHometeamId() + " = " + HOMETEAM + "._id" +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + AWAYTEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColAwayteamId() + " = " + AWAYTEAM + "._id" +
                    " INNER JOIN " + SeasonTable.getTableName() + " ON " + HOMETEAM + "." + TeamTable.getColSeasonId() + " = " + SeasonTable.getTableName() + "._id" +
                    " INNER JOIN " + LeagueTable.getTableName() + " ON " + SeasonTable.getTableName() + "." + SeasonTable.getColLeagueId() + " = " + LeagueTable.getTableName() + "._id" +
                    " GROUP BY " + PlayerRatingTable.getTableName() + "." + PlayerRatingTable.getColMatchId() +
                    " HAVING " + countOfPlayersInMatchCol + " < " + 22 +
                    " ORDER BY " + LeagueTable.getTableName() + "." + LeagueTable.getColName() + ", " + SeasonTable.getTableName() + "." + SeasonTable.getColYearBeginning() + " ASC");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static ResultSet getMatchesWithoutBothTeamsHavingPlayerRatings(Statement statement) {
        java.util.Date todayNoTime = DateHelper.setTimeOfDate(new java.util.Date(), 0, 0, 0);
        String sqlBeginningOfToday = DateHelper.getSqlDate(todayNoTime);

        try {
            return statement.executeQuery("SELECT " + HOMETEAM + "." + TeamTable.getColTeamName() + ", " + AWAYTEAM + "." + TeamTable.getColTeamName() + ", " +
                    MatchTable.getTableName() + "." + MatchTable.getColDate() + ", " + SeasonTable.getTableName() + "." + SeasonTable.getColYearBeginning() + ", " +
                    LeagueTable.getTableName() + "." + LeagueTable.getColName() +
                    " FROM " + MatchTable.getTableName() +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + HOMETEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColHometeamId() + " = " + HOMETEAM + "._id" +
                    " INNER JOIN " + TeamTable.getTableName() + " AS " + AWAYTEAM + " ON " + MatchTable.getTableName() + "." + MatchTable.getColAwayteamId() + " = " + AWAYTEAM + "._id" +
                    " INNER JOIN " + SeasonTable.getTableName() + " ON " + HOMETEAM + "." + TeamTable.getColSeasonId() + " = " + SeasonTable.getTableName() + "._id" +
                    " INNER JOIN " + LeagueTable.getTableName() + " ON " + SeasonTable.getTableName() + "." + SeasonTable.getColLeagueId() + " = " + LeagueTable.getTableName() + "._id" +
                    " WHERE " + MatchTable.getTableName() + "._id" + " NOT IN (" +
                    " SELECT " + PlayerRatingTable.getTableName() + "." + PlayerRatingTable.getColMatchId() + " FROM " + PlayerRatingTable.getTableName() + ")" +
                    " AND " + MatchTable.getTableName() + "." + MatchTable.getColDate() + " < '" + sqlBeginningOfToday + "'" +
                    " ORDER BY " + LeagueTable.getTableName() + "." + LeagueTable.getColName() + ", " + SeasonTable.getTableName() + "." + SeasonTable.getColYearBeginning() + " ASC");
        } catch (SQLException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {

        openConnection();

//        java.util.Date date = DateHelper.setDate(2019, 01, 01);
//        getMatchesWithoutPredictions(date); //should get 49 back

//        BetResultsTotalled brt = getResultsOfPredictions(null, null, null);

        closeConnection();

    }

}
