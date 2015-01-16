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

import java.text.DecimalFormat;
import java.util.Date;
// import java.text.DecimnalFormat;


import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
// import org.bukkit.Bukkit;
import org.bukkit.Location;

public class Main extends JavaPlugin implements Listener {

  private String localMessage = "";
  private String recentPlayer = "";
  private String recentPlayerIP = "";
  private Location recentPlayerLocation;
  private String locationMessage = "";
  private double playerX;
  private double playerY;
  private double playerZ;
  private String Xcoords = "";
  private String Ycoords = "";
  private String Zcoords = "";
  private boolean recentJoin = false;
  // private String[] exemptionList = {"Banana_Skywalker", "JeannieInABottle"};
  private static final String entryMessage = "Server's up, time to get crafting!";
  private static final String exitMessage = "The server has joined the choir invisibule";
  private static final boolean TWITTER_CONFIGURED = false;
  private static final String API_KEY = "XXXX";
  private static final String API_SECRET = "YYYY";
  private static final String token = "ZZZ";
  private static final String secret = "ABABAB";
  private static Twitter twitter;
  
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
    recentJoin = true;
    recentPlayer = event.getPlayer().getName();
    recentPlayerIP = event.getPlayer().getAddress().getHostString();
    recentPlayerLocation = event.getPlayer().getLocation();
    // TODO make this a separate method and check for presence in exemption list
    playerX = recentPlayerLocation.getX();
    playerY = recentPlayerLocation.getY();
    playerZ = recentPlayerLocation.getZ();
    DecimalFormat df = new DecimalFormat("#.##");
    Xcoords = df.format(playerX);
    Ycoords = df.format(playerY);
    Zcoords = df.format(playerZ);
    localMessage = setLocalMessage(recentJoin);
    getLogger().info(localMessage);
    locationMessage = "X:" + Xcoords + " Y:" + Ycoords + " Z:" + Zcoords;
    getLogger().info(locationMessage);
    updateStatus(twitter, recentPlayer + " flew in." + localMessage + "\n" + locationMessage);
    localMessage = "";
  }
  
  @EventHandler
  public void onLogout (PlayerQuitEvent event) throws Exception {
    recentJoin = false;
    recentPlayer = event.getPlayer().getName();
    recentPlayerIP = event.getPlayer().getAddress().getHostString();
    recentPlayerLocation = event.getPlayer().getLocation();
    // TODO make this a separate method and check for presence in exemption list
    playerX = recentPlayerLocation.getX();
    playerY = recentPlayerLocation.getY();
    playerZ = recentPlayerLocation.getZ();
    DecimalFormat df = new DecimalFormat("#.##");
    Xcoords = df.format(playerX);
    Ycoords = df.format(playerY);
    Zcoords = df.format(playerZ);
    localMessage = setLocalMessage(recentJoin);
    getLogger().info(localMessage);
    locationMessage = "X:" + Xcoords + " Y:" + Ycoords + " Z:" + Zcoords;
    getLogger().info(locationMessage);
    updateStatus(twitter, recentPlayer + " flew away." + localMessage + "\n" + locationMessage);
    localMessage = "";
  }
  
  private String setLocalMessage (boolean recentJoin) {
    if (isLocal(recentPlayerIP)) {
      if (recentJoin) {
        return ("\n" + recentPlayer + " is a local person.");
      } else {
        return ("\n" + recentPlayer + " was a local person."); 
      }
    } else {
      if (recentJoin) {
        return ("\n" + recentPlayer + " is not local!");
      } else {
        return ("\n" + recentPlayer + " was not local."); 
      }
    }
  }
  
  private boolean isLocal(String recentPlayerIP) {
    if (recentPlayerIP.startsWith("192.168")) {
    return true;
    }
    else {
    return false;
    }
  }
  
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

  private void updateStatus(Twitter twitter, String testMessage) {
    if (twitter != null) {
      try {
        twitter.updateStatus(testMessage + "\n" + new Date());
      } catch (TwitterException e) {
        getLogger().info("Twitter is broken because of " + e);
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
