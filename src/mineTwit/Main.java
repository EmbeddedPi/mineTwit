/******************************************************************************
 * Copyright (c) 2014 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 *    EmbeddedPi - Converted scope of usage to Minecraft server notification
 *****************************************************************************/
package mineTwit;

import java.util.Date;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class Main extends JavaPlugin implements Listener {

  private String localMessage = "";
  private String recentPlayer = "";
  private String recentPlayerIP = "";
  private static final String entryMessage = "On it like a car bonnet.";
  private static final String exitMessage = "The server has joined the choir invisibule";
  private static final boolean TWITTER_CONFIGURED = false;
  private static final String API_KEY = "XXXX";
  private static final String API_SECRET = "YYYY";
  private static final String token = "ZZZ";
  private static final String secret = "ABABAB";
  private static Twitter twitter;
  
/*
  public void main(String[] args) throws Exception {
    getLogger().info("mineTwit is running main method");
    twitter = setupTwitter();
    updateStatus(twitter, entryMessage);
    sleep();
    getLogger().info("finished main method");
  }
*/ 
  
  @Override
  public void onEnable() {
    // Register listener
    getServer().getPluginManager().registerEvents(this, this);
    // Set up Twitter
    try {
      twitter = setupTwitter();
      updateStatus(twitter, entryMessage);
      } catch (TwitterException e) {
      getLogger().info("Twitter is broken because of " + e);
      } finally {
      getLogger().info("mineTwit goes tweet tweet");
      }
  }
  
  @Override
  public void onDisable() {
    // Server down notification
    try {
      twitter = setupTwitter();
      updateStatus(twitter, exitMessage);
      } catch (TwitterException e) {
      getLogger().info("Twitter is broken because of " + e);
      } finally {
      getLogger().info("mineTwit has fallen off the perch");
      }
  }
  
  @EventHandler
  public void onLogin(PlayerJoinEvent event) throws Exception {
    recentPlayer = event.getPlayer().getName();
    recentPlayerIP = event.getPlayer().getAddress().getHostString();
    // Check whether internal or external IP address
    if (recentPlayerIP.startsWith("192.168")) {
      localMessage = recentPlayer + " is a local person."; 
    }
    else {
      localMessage = recentPlayer + " is not local.";
    }
    getLogger().info(recentPlayer + " flew in");
    getLogger().info(localMessage);
    updateStatus(twitter, recentPlayer + " flew in. " + localMessage);
   }
  
  @EventHandler
  public void onLogout (PlayerQuitEvent event) throws Exception {
    recentPlayer = event.getPlayer().getName();
    recentPlayerIP = event.getPlayer().getAddress().getHostString();
    // Check whether internal or external IP address
    if (recentPlayerIP.startsWith("192.168")) {
      localMessage = recentPlayer + " was a local person."; 
    }
    else {
      localMessage = recentPlayer + " was not local.";
    }
    getLogger().info(recentPlayer + " flew away");
    getLogger().info(localMessage);
    updateStatus(twitter, recentPlayer + " flew away. " + localMessage);
   }
  
  /*
  private static void sleep() throws InterruptedException {
    for (;;) {
      Thread.sleep(500);
    }
  }
*/

  private static Twitter setupTwitter() throws TwitterException {
    if (TWITTER_CONFIGURED) {
      TwitterFactory factory = new TwitterFactory();
      final Twitter twitter = factory.getInstance();
      AccessToken accessToken = loadAccessToken();
      authenticateTwitter(accessToken, twitter);
      return twitter;
    }
    return null;
  }

  private static void updateStatus(Twitter twitter, String testMessage) {
    if (twitter != null) {
      try {
        twitter.updateStatus(testMessage + " : Message sent from test server : " + new Date());
      } catch (TwitterException e) {
        throw new RuntimeException(e);
      }
    }
  }
  
  private static void authenticateTwitter(AccessToken accessToken, Twitter twitter) {
    twitter.setOAuthConsumer(API_KEY, API_SECRET);
    twitter.setOAuthAccessToken(accessToken);
  }

  private static AccessToken loadAccessToken() {
    String token = Main.token;
    String tokenSecret = secret;
    return new AccessToken(token, tokenSecret);
  }

}
