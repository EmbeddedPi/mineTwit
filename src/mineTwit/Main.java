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
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

public class Main extends JavaPlugin implements Listener {

  private String localMessage = "";
  private String recentPlayer = "";
  private String recentPlayerIP = "";
  private Location recentPlayerLocation;
  private String locationMessage = "";
  private boolean recentJoin = false;
  private String[] exemptionList = {"Banana_Skywalker", "JeannieInABottle"};
  private static final String entryMessage = "Server's up, time to get crafting!";
  private static final String exitMessage = "The server has joined the choir invisibule.";
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
    locationMessage = parseLocation(recentPlayerLocation);
    localMessage = setLocalMessage(recentJoin);
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
    locationMessage = parseLocation(recentPlayerLocation);
    localMessage = setLocalMessage(recentJoin);
    getLogger().info(locationMessage);
    updateStatus(twitter, recentPlayer + " flew away." + localMessage + "\n" + locationMessage);
    localMessage = "";
  }
  
  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    Player player = event.getPlayer();
    Block block = event.getBlock();
    Material mat = block.getType();
    // Tweet who placed which block.
    updateStatus(twitter, player.getName() + " placed a block of " + mat.toString().toLowerCase() + ".");
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
    // Check whether PI address is either coming from router hence WAN
    if (recentPlayerIP.equals("192.168.1.1")) {
      return false;
    }
    else if (recentPlayerIP.startsWith("192.168")) {
      return true;
    }
    else {
      return false;
    }
  }
  
  private String parseLocation(Location location) {
    // 5 records to include currently unused pitch and yaw
    String playerLocation[] = {"","","","",""};
    String locationString = "";
    Boolean exemption = false;
    DecimalFormat df = new DecimalFormat("#.##");
    playerLocation[0] = df.format(location.getX());
    playerLocation[1] = df.format(location.getY());
    playerLocation[2] = df.format(location.getZ());
    /* Possibly use in future
    playerLocation[3] = df.format(location.getPitch());
    playerLocation[4] = df.format(location.getYaw());
    */
    for (String e : exemptionList) {
      if (e.contains(recentPlayer)) {
        exemption = true;
      }
    }
    if (exemption) {
      locationString = recentPlayer + " is sneaky and can't be seen!";
      getLogger().info(recentPlayer + " is exempt from co-ord display");
    }
    else {
      locationString = "X: " + playerLocation[0] + " Y: " + playerLocation[1] + " Z: " + playerLocation[2];
      getLogger().info(recentPlayer + " is not exempt from co-ord display");
    }
    return locationString;
  }
  
  private Twitter setupTwitter() throws TwitterException {
    if (TWITTER_CONFIGURED) {
      TwitterFactory factory = new TwitterFactory();
      final Twitter twitter = factory.getInstance();
      AccessToken accessToken = loadAccessToken();
      authenticateTwitter(accessToken, twitter);
      return twitter;
    }
    else {
      getLogger().info("Twitter is switched off you doughnut.");
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
