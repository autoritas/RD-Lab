/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.autoritas.idi;

import com.cybozu.labs.langdetect.Detector;
import com.cybozu.labs.langdetect.DetectorFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import twitter4j.Paging;
import twitter4j.RateLimitStatus;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.conf.ConfigurationBuilder;
import twitter4j.json.DataObjectFactory;

/**
 *
 * @author kicorangel
 */
public class TWTimelineCrawler {
    public static String KEY = "";              // You should put here your Twitter KEY
    public static String SECRET = "";           // You should put here your Twitter SECRET
    public static String ACCESSTOKEN = "";      // You should put here your Twitter ACCESS TOKEN
    public static String ACCESSSECRET = "";     // You should put here your Twitter ACCESS SECRET
    
    private static int NUM_TWEETS = 0;       // You should put here the number of tweets you want to collect
    
    private static String LANG = "es";          // You should put here the language you want to filter by
    
   // private static String USERS = "kicorangel;xllinares";           // You should put here a list of users separated by ;
    private static String USER = "";           // You should put here a list of users separated by ;
    
    private static String LANG_MODELS = "../profiles";
    
    public static void main(String[] args) throws InterruptedException, TwitterException, Exception {
        
        
        //String []Users = USERS.split(";");

        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("-nt")) {
                NUM_TWEETS = Integer.valueOf(args[i+1]);
            } else if (args[i].equals("-u")) {
                USER = args[i+1];
            } else if (args[i].equals("-lm")) {
                LANG_MODELS = args[i+1];
            } else if (args[i].equals("-t_key")) {
                KEY = args[i+1];
            } else if (args[i].equals("-t_secret")) {
                SECRET = args[i+1];
            } else if (args[i].equals("-ta_token")) {
                ACCESSTOKEN = args[i+1];
            } else if (args[i].equals("-ta_secret")) {
                ACCESSSECRET = args[i+1];
            } else if (args[i].equals("-l")) {
                LANG = args[i+1];
            }

        }
        
        DetectorFactory.loadProfile(LANG_MODELS);
        
        
        
        System.err.println("Loading @" + USER + "'s user timeline...");
        ArrayList<String> Tweets = ProcessTimeLine(USER);
           
        // Here you should save the tweets per user
        for (String t : Tweets) {
            System.out.println(t);
        }
    }
    
    
    
    
    
    private static ArrayList<String> ProcessTimeLine(String user) throws InterruptedException, TwitterException {
        ArrayList<String> Tweets = new ArrayList<String>();
        
        ConfigurationBuilder cb = new ConfigurationBuilder();
        cb.setDebugEnabled(true)
          .setOAuthConsumerKey(KEY)
          .setOAuthConsumerSecret(SECRET)
          .setOAuthAccessToken(ACCESSTOKEN)
          .setOAuthAccessTokenSecret(ACCESSSECRET);
        cb.setJSONStoreEnabled(true);


        // gets Twitter instance with default credentials
        boolean bWait = true;
        Twitter twitter = new TwitterFactory(cb.build()).getInstance();
        do
        {
            try {
                Map<String, RateLimitStatus> oRT = twitter.getRateLimitStatus();
                RateLimitStatus rateLimit = oRT.get("/statuses/user_timeline");
                int remaining = rateLimit.getRemaining();
                System.err.print("(Remaining API calls: " + remaining + ")");
                int remainingTime = rateLimit.getSecondsUntilReset();

                if (remaining<=NUM_TWEETS/200 + 1)
                {
                    System.err.println("Waiting " + remainingTime + " seconds");
                    Thread.sleep(remainingTime * 1000);
                }
                else
                    bWait = false;

            } catch (Exception te) {
                if (te.toString().toLowerCase().contains("rate limit") && !te.toString().toLowerCase().contains("bad authentication data")) {
                    System.err.println("Waiting 60s");
                    Thread.sleep(60 * 1000);
                } else {
                    bWait = false;
                }
            }
        }while (bWait);
        
        try {
            Detector detector = DetectorFactory.create();
            List<Status> statuses;

            int iPage = 1;
            int iTweets = 0;
            do {

                int iPageSize = 0;
                if (iTweets+200<NUM_TWEETS) {
                    iPageSize = 200;
                } else {
                    iPageSize = NUM_TWEETS - iTweets;
                }
                statuses = twitter.getUserTimeline(user,new Paging(iPage,iPageSize));

                for (Status status : statuses) {
                     
                    String sStatusId = "-1";
                    try
                    {
                        if ((status.getRetweetedStatus()!=null) && (status.getRetweetedStatus().getUser()!=null)) {
                            continue;
                        }

                        
                        try { 
                             detector.append(Simplify(status.getText()));
                            if (detector.detect().equalsIgnoreCase("es")) {
                                String sStatusJSON = DataObjectFactory.getRawJSON(status);
                                Tweets.add(sStatusJSON);
                            }
                        } catch (Exception exl) {}
                    }
                    catch (Exception ex)
                    {
                        System.err.println("ERROR in status id " + sStatusId);
                    }
                    
                    iTweets++;
                }
                iPage++;
            } while (statuses.size()>0 && iTweets<NUM_TWEETS);

        } catch (TwitterException te) {
            te.printStackTrace();
            System.err.println("Failed to get timeline: " + te.getMessage());
        } catch(Exception ex) {
            
        }
        
        System.err.println("..." + Tweets.size() + " tweets.");
        
        return Tweets;
    }
    
    
    private static String Simplify(String text) {
        while (text.contains("@")) {
            int iIni = text.indexOf("@");
            int iEnd = iIni + 1;
            boolean bEnd = false;
            
            if (iEnd>=text.length()) {
                bEnd = true;
            }
            
            while (!bEnd) {
                char c = text.charAt(iEnd);
                
//                if (c==' ' || c==',' || c=='.' || c=='\t' || c=='\r' || c=='\n' || c==';' || c==':' || c=='@' || c=='#' || c=='(' || c==')' || c=='!' || c=='?') {
                if (!( (c>='a' && c<='z') || (c>='A' && c<='Z') || (c>='0' && c<='9') || (c=='_' || c=='-') )) {
                    bEnd = true;
                } else {
                    iEnd++;
                    
                    if (iEnd>=text.length()) {
                        bEnd = true;
                    }
                
                }
            }
            String sUser = text.substring(iIni, iEnd);
            text = text.replaceAll(sUser, "");
        }
        
        while (text.contains("#")) {
            int iIni = text.indexOf("#");
            int iEnd = iIni + 1;
            boolean bEnd = false;
            while (!bEnd) {
                char c = text.charAt(iEnd);
                
//                if (c==' ' || c==',' || c=='.' || c=='\t' || c=='\r' || c=='\n'  || c==';' || c==':' || c=='@' || c=='#' || c=='(' || c==')' || c=='!' || c=='?') {
                if (!( (c>='a' && c<='z') || (c>='A' && c<='Z') || (c>='0' && c<='9') || (c=='_' || c=='-') )) {
                    bEnd = true;
                } else {
                    iEnd++;
                    
                    if (iEnd>=text.length()) {
                        bEnd = true;
                    }
                }
            }
            String sUser = text.substring(iIni, iEnd);
            text = text.replaceAll(sUser, "");
        }
        
        return text;
    }
}
