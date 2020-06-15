package com.petermarshall.database.datasource;

import com.petermarshall.database.dbTables.*;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DS_Main {
    //used for calls to db to ensure consistent naming
    static final String HOMETEAM = "hometeam";
    static final String AWAYTEAM = "awayteam";

    //    private static final String CONNECTION_NAME = "jdbc:sqlite:C:\\Databases\\footballMatchesREFACTOR.db";
//public static final String TEST_CONNECTION_NAME = "jdbc:sqlite:C:\\Databases\\footballMatchesTEST.db";
    private static final String CONNECTION_NAME = "jdbc:mysql://localhost/footballtest2?serverTimezone=UTC";
    public static final String TEST_CONNECTION_NAME = "jdbc:mysql://localhost/testfootballtest?serverTimezone=UTC";
    public static Connection connection;

    public static boolean isOpen() {
        try {
            return connection == null || !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /*
     * To be called before every use of this class, followed by a call to closeConnection when all db
     * work has been completed.
     */
    public static boolean openProductionConnection() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(CONNECTION_NAME, Keys.user, Keys.password);
            return true;
        } catch (SQLException | ClassNotFoundException e) {
            System.out.println(e.getMessage());
            return false;
        }
    }

    public static boolean openTestConnection() {
        try {
            connection = DriverManager.getConnection(TEST_CONNECTION_NAME, Keys.user, Keys.password);
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
    public static void initDB() {
        try (Statement statement = connection.createStatement()) {

            statement.execute("CREATE TABLE IF NOT EXISTS " + LeagueTable.getTableName() +
                    " (" + LeagueTable.getColName() + " text NOT NULL," + " _id int NOT NULL, PRIMARY KEY(_id), " +
                    "UNIQUE KEY unique_id (_id)," +
                    "UNIQUE KEY unique_league (" + LeagueTable.getColName() + "(50)))");

            statement.execute("CREATE TABLE IF NOT EXISTS " + TeamTable.getTableName() + "" +
                    " (" + TeamTable.getColTeamName() + " text NOT NULL, " + TeamTable.getColLeagueId() + " int NOT NULL, "
                    + "_id int NOT NULL, " +
                    " PRIMARY KEY(_id), " +
                    " UNIQUE KEY unique_team (" + TeamTable.getColTeamName() + "(50), " + TeamTable.getColLeagueId() + ")," +
                    " KEY league_id_idx (" + TeamTable.getColLeagueId() + "), " +
                    " CONSTRAINT league_id FOREIGN KEY (" + TeamTable.getColLeagueId() + ") REFERENCES " + LeagueTable.getTableName() + " (_id))");


            statement.execute("CREATE TABLE IF NOT EXISTS " + MatchTable.getTableName() + " (" +
                    MatchTable.getColHomeScore() + " int DEFAULT NULL, " + MatchTable.getColAwayScore() + " int DEFAULT NULL, " +
                    MatchTable.getColHomeXg() + " double DEFAULT NULL, " + MatchTable.getColAwayXg() + " double DEFAULT NULL, " +
                    MatchTable.getColDate() + " text NOT NULL, " + MatchTable.getColHomeWinOdds() + " double DEFAULT NULL, " +
                    MatchTable.getColDrawOdds() + " double DEFAULT NULL, " + MatchTable.getColAwayWinOdds() + " double DEFAULT NULL, " +
                    MatchTable.getColFirstScorer() + " int DEFAULT -1, " + MatchTable.getColIsPostponed() + " int DEFAULT 0, " +
                    MatchTable.getColHometeamId() + " int NOT NULL, " + MatchTable.getColAwayteamId() + " int NOT NULL, " +
                    MatchTable.getColSeasonYearStart() + " int NOT NULL, _id int NOT NULL, " +
                    MatchTable.getColPredictedLive() + " int DEFAULT NULL, " +
                    MatchTable.getColSofascoreId() + " int DEFAULT NULL, " +
                    "PRIMARY KEY(_id), " +
                    "UNIQUE (" + MatchTable.getColHometeamId() + "," + MatchTable.getColAwayteamId() + "," + MatchTable.getColSeasonYearStart() + "), " +
                    "FOREIGN KEY (" + MatchTable.getColAwayteamId() + ") REFERENCES " + TeamTable.getTableName() + "(_id), " +
                    "FOREIGN KEY (" + MatchTable.getColHometeamId() + ") REFERENCES " + TeamTable.getTableName() + "(_id))"
            );

            statement.execute("CREATE TABLE IF NOT EXISTS " + PlayerRatingTable.getTableName() + " (" +
                    PlayerRatingTable.getColMins() + " int NOT NULL, " +
                    PlayerRatingTable.getColRating() + " double NOT NULL, " +
                    PlayerRatingTable.getColMatchId() + " int NOT NULL, " + PlayerRatingTable.getColTeamId() + " int NOT NULL, " +
                    PlayerRatingTable.getColPlayerName() + " text NOT NULL, " +
                    "UNIQUE KEY unique_player_rating (" + PlayerRatingTable.getColMatchId() + "," + PlayerRatingTable.getColPlayerName() + "(50)," + PlayerRatingTable.getColTeamId() + ")," +
                    "KEY match_id_idx (" + PlayerRatingTable.getColMatchId() + "), " +
                    "KEY team_id_idx (" + PlayerRatingTable.getColTeamId() + "), " +
                    "CONSTRAINT match_id_played_in FOREIGN KEY (" + PlayerRatingTable.getColMatchId() + ") REFERENCES " + MatchTable.getTableName() + " (_id), " +
                    "CONSTRAINT team_id FOREIGN KEY (" + PlayerRatingTable.getColTeamId() + ") REFERENCES " + TeamTable.getTableName() + " (_id), " +
                    "CONSTRAINT mins_in_range CHECK (((" + PlayerRatingTable.getColMins() + " <= 90) and (" + PlayerRatingTable.getColMins() + " >= 0))), " +
                    "CONSTRAINT rating_in_range CHECK (((" + PlayerRatingTable.getColRating() + " <= 10) and (" + PlayerRatingTable.getColRating() + " >= 0))))");


            statement.execute("CREATE TABLE IF NOT EXISTS " + BetTable.getTableName() + " (" +
                    BetTable.getColResultBetOn() + " int NOT NULL, " +
                    BetTable.getColOdds() + " double NOT NULL, " + BetTable.getColStake() + " double NOT NULL, " +
                    BetTable.getColMatchId() + " int NOT NULL, " + BetTable.getColBetPlacedWith() + " text, " +
                    " KEY match_id_idx (" + BetTable.getColMatchId() + "), " +
                    " CONSTRAINT match_id FOREIGN KEY (" + BetTable.getColMatchId() + ") REFERENCES " + MatchTable.getTableName() + "(_id), " +
                    "CONSTRAINT result_in_range CHECK (((" + BetTable.getColResultBetOn() + " >= 1) and (" + BetTable.getColResultBetOn() + " <= 3)))" +
                    ")");

            statement.execute("CREATE TABLE IF NOT EXISTS " + PredictionTable.getTableName() + " (" +
                    PredictionTable.getColDate() + " text NOT NULL, " + PredictionTable.getColWithLineups() + " tinyint(1) NOT NULL, " +
                    PredictionTable.getColHomePred() + " double NOT NULL, " + PredictionTable.getColDrawPred() + " double NOT NULL, " +
                    PredictionTable.getColAwayPred() + " double NOT NULL, " + PredictionTable.getColBookieName() + " text, " +
                    PredictionTable.getColHOdds() + " double DEFAULT -1, " + PredictionTable.getColDOdds() + " double DEFAULT -1, " +
                    PredictionTable.getColAOdds() + " double DEFAULT -1, " + PredictionTable.getColMatchId() + " int NOT NULL, " +
                    " KEY match_id_idx (" + PredictionTable.getColMatchId() + "), " +
                    " CONSTRAINT match_id_f_key FOREIGN KEY (" + PredictionTable.getColMatchId() + ") REFERENCES " + MatchTable.getTableName() + "(_id))");


            statement.execute("CREATE TABLE IF NOT EXISTS " + LogTable.getTableName() + " (" +
                    LogTable.getColDatetime() + " text, " + LogTable.getColInfo() + " text)");


            //TODO: note, these statements are only commented to test the mysql connection before spending the effort to change these. Currently configured for sqlite

//            statement.execute("CREATE TABLE IF NOT EXISTS " + LeagueTable.getTableName() +
//                    " (" + LeagueTable.getColName() + " TEXT NOT NULL UNIQUE," + " _id INTEGER NOT NULL UNIQUE, PRIMARY KEY(_id) )");
//
//            statement.execute("CREATE TABLE IF NOT EXISTS " + TeamTable.getTableName() +
//                    " (" + TeamTable.getColTeamName() + " TEXT NOT NULL, " + TeamTable.getColLeagueId() + " INTEGER NOT NULL, "
//                    + "_id INTEGER NOT NULL UNIQUE, " +
//                    " UNIQUE(" + TeamTable.getColTeamName() + ", " + TeamTable.getColLeagueId() + ")," +
//                    " PRIMARY KEY(_id), " +
//                    "FOREIGN KEY(" + TeamTable.getColLeagueId() + ") REFERENCES " + LeagueTable.getTableName() + "(_id))");
//
//            statement.execute("CREATE TABLE IF NOT EXISTS " + MatchTable.getTableName() +
//                    " (" + MatchTable.getColHomeScore() + " INTEGER, " + MatchTable.getColAwayScore() + " INTEGER, " +
//                    MatchTable.getColHomeXg() + " REAL, " + MatchTable.getColAwayXg() + " REAL, " +
//                    MatchTable.getColDate() + " TEXT NOT NULL, " + MatchTable.getColHomeWinOdds() + " REAL, " +
//                    MatchTable.getColDrawOdds() + " REAL, " + MatchTable.getColAwayWinOdds() + " REAL, " +
//                    MatchTable.getColFirstScorer() + " INTEGER DEFAULT -1, " + MatchTable.getColIsPostponed() + " INTEGER DEFAULT 0, " +
//                    MatchTable.getColHometeamId() + " INTEGER NOT NULL, " + MatchTable.getColAwayteamId() + " INTEGER NOT NULL, " +
//                    MatchTable.getColSeasonYearStart() + " INTEGER NOT NULL, _id INTEGER NOT NULL UNIQUE, " +
//                    MatchTable.getColPredictedLive() + " INTEGER CHECK(" + MatchTable.getColPredictedLive() + " == 0 OR " + MatchTable.getColPredictedLive() + " == 1), " +
//                    MatchTable.getColSofascoreId() + " INTEGER, " +
//                    "FOREIGN KEY(" + MatchTable.getColHometeamId() + ") REFERENCES " + TeamTable.getTableName() + "(_id), " +
//                    "FOREIGN KEY(" + MatchTable.getColAwayteamId() + ") REFERENCES " + TeamTable.getTableName() + "(_id), " +
//                    "UNIQUE(" + MatchTable.getColHometeamId() + ", " + MatchTable.getColAwayteamId() + ", " + MatchTable.getColSeasonYearStart() + "), " +
//                    "PRIMARY KEY(_id))");
//
//            statement.execute("CREATE TABLE IF NOT EXISTS " + PlayerRatingTable.getTableName() +
//                    " (" + PlayerRatingTable.getColMins() + " INTEGER NOT NULL CHECK (" + PlayerRatingTable.getColMins() + " <= 90 " +
//                    " AND " + PlayerRatingTable.getColMins() + " > 0), " +
//                    PlayerRatingTable.getColRating() + " REAL NOT NULL CHECK (" + PlayerRatingTable.getColRating() + " <= 10 " +
//                    "AND " + PlayerRatingTable.getColRating() + " > 0), " +
//                    PlayerRatingTable.getColMatchId() + " INTEGER NOT NULL, " + PlayerRatingTable.getColTeamId() + " INTEGER NOT NULL, " +
//                    PlayerRatingTable.getColPlayerName() + " TEXT NOT NULL, " +
//                    "FOREIGN KEY(" + PlayerRatingTable.getColTeamId() + ") REFERENCES " + TeamTable.getTableName() + "(_id), " +
//                    "FOREIGN KEY(" + PlayerRatingTable.getColMatchId() + ") REFERENCES " + MatchTable.getTableName() + "(_id), " +
//                    "UNIQUE(" + PlayerRatingTable.getColPlayerName() + "," + PlayerRatingTable.getColMatchId() + "))");
//
//            statement.execute("CREATE TABLE IF NOT EXISTS " + BetTable.getTableName() +
//                    " (" + BetTable.getColResultBetOn() + " INTEGER NOT NULL " +
//                    "CHECK(" + BetTable.getColResultBetOn() + " >= 1 AND " + BetTable.getColResultBetOn() + " <= 3), " +
//                    BetTable.getColOdds() + " REAL NOT NULL, " + BetTable.getColStake() + " REAL NOT NULL, " +
//                    BetTable.getColMatchId() + " INTEGER NOT NULL, _id INTEGER NOT NULL UNIQUE," +
//                    "FOREIGN KEY(" + BetTable.getColMatchId() + ") REFERENCES " + MatchTable.getTableName() + "(_id))");
//
//            statement.execute("CREATE TABLE IF NOT EXISTS " + LogTable.getTableName() +
//                    "( " + LogTable.getColDatetime() + " TEXT, " + LogTable.getColInfo() + " TEXT)");

        } catch (SQLException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
    }
}
