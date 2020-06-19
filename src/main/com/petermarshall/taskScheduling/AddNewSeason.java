package com.petermarshall.taskScheduling;

import com.petermarshall.DateHelper;
import com.petermarshall.mail.SendEmail;
import com.petermarshall.scrape.classes.League;
import com.petermarshall.scrape.classes.LeagueIdsAndData;

public class AddNewSeason {
    //to be scheduled yearly in august.
    public static void addIdsReminder() {
        SendEmail.sendOutEmail("Pls add season ids", "\n___________________________________________________\n\n" +
                "It's now the summer and time to add in new season Ids for sofascore." +
                "You may also have to add some new team name transitions in the Team.makeTeamNamesCompatible()." +
                "\n\nThis does not need to be done now, but be aware that some errors may be thrown by new teams having different names in SofaScore and Understat." +
                "\n\n___________________________________________________\n");
    }

    //called only after new season ids have been added
    public static void addNewSeasons() {
        for (League l: League.getAllLeagues()) {
            //note: DateHelper method changes to new season at start of august.
            l.scrapeOneSeason(DateHelper.getStartYearForCurrentSeason());
        }
    }
}