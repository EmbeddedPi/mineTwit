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

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;
import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.RateLimitStatus;

import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.event.Listener;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityTameEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.entity.LivingEntity;

public class Main extends JavaPlugin implements Listener {

  private String localMessage = "";
  private String recentPlayer = "";
  private String recentPlayerIP = "";
  private Location recentPlayerLocation;
  private String locationMessage = "";
  private boolean recentJoin = false;
  //TODO Consider adding this into config settings
  private String[] exemptionList = {"Banana_Skywalker", "JeannieInABottle"}; 
  private static final String entryMessage = "Server's up, time to get crafting!";
  private static final String exitMessage = "The server has joined the choir invisibule.";
  private static Twitter twitter;
  private class twitterSettings {
    boolean status;
    String apiKey;
    String apiSecret;
    String token;
    String secret;
  }
  private class rateLimits {
    boolean limited;
    @SuppressWarnings("unused")
    String endpointName;
    long resetTime;
    String resetDate;
  }
  rateLimits rateLimitStatus = new rateLimits();
  twitterSettings twitterSettings = new twitterSettings(); 
  private class notificationList {
    String type;
    boolean status;
  }
  notificationList[] myNotifications = new notificationList[9]; 
  
  @Override
  public void onEnable() {
    // Register listener
    getServer().getPluginManager().registerEvents(this, this);
    //Set up notifications
    //TODO Possibly remove this later as forces return to default upon load
    initialiseNotifications();
    twitterSettings = loadConfiguration();
    resetRateLimit(rateLimitStatus);
    // Set up Twitter
    try {
      twitter = setupTwitter(twitterSettings);
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
    updateStatus(twitter, exitMessage);
  }
  
  public twitterSettings loadConfiguration() { 
    // Create virtual config file
    File configFile = new File(getDataFolder(), "config.yml");
    twitterSettings configSettings = new twitterSettings();
    /*
     * If this is the first time that plugin has run or config file not present
     * then copy from default config file.
     */
    if (!configFile.exists()) {
      getLogger().info("[loadConfiguration]Plugin hasn't been configured so creating config");
      this.getConfig().options().copyDefaults(true);
      configFile.getParentFile().mkdirs();
      copy(getResource("config.yml"), configFile);
      //Assign variables directly as default dummy values
      configSettings.status = this.getConfig().getBoolean("Twitter.TWITTER_CONFIGURED");
      configSettings.apiKey = this.getConfig().getString("Twitter.API_KEY");
      configSettings.apiSecret = this.getConfig().getString("Twitter.API_SECRET");
      configSettings.token = this.getConfig().getString("Twitter.token");
      configSettings.secret = this.getConfig().getString("Twitter.secret");
    } else {
      getLogger().info("[loadConfiguration]config file already exists");  
      //Read config file and assign values
      /*
       * Look at handling
       * org.bukkit.configuration.InvalidConfigurationException
       * as this resets all value if any one string has errors
       * Boolean values are fine
       */
      this.getConfig().options().copyDefaults(false);
      configSettings.status = this.getConfig().getBoolean("Twitter.TWITTER_CONFIGURED");
      configSettings.apiKey = this.getConfig().getString("Twitter.API_KEY");
      configSettings.apiSecret = this.getConfig().getString("Twitter.API_SECRET");
      configSettings.token = this.getConfig().getString("Twitter.token");
      configSettings.secret = this.getConfig().getString("Twitter.secret");
    } 
    //Write back to file to catch any invalid parameters reset back to default
    this.getConfig().set("Twitter.TWITTER_CONFIGURED", configSettings.status);
    this.getConfig().set("Twitter.API_KEY", configSettings.apiKey);
    this.getConfig().set("Twitter.API_SECRET", configSettings.apiSecret);
    this.getConfig().set("Twitter.token", configSettings.token);
    this.getConfig().set("Twitter.secret", configSettings.secret); 
    saveConfig();
    return configSettings;
  }
  
  @EventHandler
  public void onLogin(PlayerJoinEvent event) throws Exception {
    if (myNotifications[0].status) {
      recentJoin = true;
      recentPlayer = event.getPlayer().getName();
      recentPlayerIP = event.getPlayer().getAddress().getHostString();
      recentPlayerLocation = event.getPlayer().getLocation();
      locationMessage = parseLocation(recentPlayerLocation);
      localMessage = setLocalMessage(recentJoin);
      getLogger().info(locationMessage);
      updateStatus(twitter, recentPlayer + " flew in." + localMessage + "\n" + locationMessage);
      localMessage = "";
    } else {
      return;
    } 
  }
  
  @EventHandler
  public void onLogout (PlayerQuitEvent event) throws Exception {
    if (myNotifications[0].status) {
      recentJoin = false;
      recentPlayer = event.getPlayer().getName();
      recentPlayerIP = event.getPlayer().getAddress().getHostString();
      recentPlayerLocation = event.getPlayer().getLocation();
      locationMessage = parseLocation(recentPlayerLocation);
      localMessage = setLocalMessage(recentJoin);
      getLogger().info(locationMessage);
      updateStatus(twitter, recentPlayer + " flew away." + localMessage + "\n" + locationMessage);
      localMessage = "";
    } else {
      return;
    } 
  }
   
  @Override
  public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {    
    if (cmd.getName().equalsIgnoreCase("setNotification")) { 
      getLogger().info("I've recognised a setNotification command");
      // Check a correct number of arguments
      if (args.length < 2) {
        sender.sendMessage("This needs two arguments!");
        return false;
      } else if (args.length >2) {
        sender.sendMessage("Calm down, too many arguments!");
        return false;
      } else {
        // Check first argument is a valid command
        for (int i=0; i<myNotifications.length; i++) {
          if (myNotifications[i].type.equalsIgnoreCase(args[0])) {
            //sender.sendMessage(myNotifications[i].type + " matches " + args[0]);
            // Check second argument is valid boolean
            if (args[1].equalsIgnoreCase("false")) {
              // Switch it off 
              myNotifications[i].status = false;
              sender.sendMessage(args[0] + " set to " + args[1]);
              return true;
            } else if (args[1].equalsIgnoreCase("true")) {
              // Switch it on 
              myNotifications[i].status = true;
              sender.sendMessage(args[0] + " set to " + args[1]);
              return true;
            } else {
              sender.sendMessage("Status needs to be true or false, " + args[1] + " is not a valid argument.");
              return false;
            }
          }
        }
        // Loop found no matching type
        sender.sendMessage("Not a valid notification type");   
        return false;
      }
    } else if (cmd.getName().equalsIgnoreCase("listNotification")) {
      for (int i=0; i<myNotifications.length; i++) {
        sender.sendMessage(myNotifications[i].type + " is " + myNotifications[i].status);
      }
      return true;     
    } else if (cmd.getName().equalsIgnoreCase("updateMineTwitConfig")) {
      twitterSettings = updateConfig(twitterSettings);
      try {
        twitter = setupTwitter(twitterSettings);
        updateStatus(twitter, entryMessage);
        } catch (TwitterException e) {
        getLogger().info("Twitter is broken because of " + e);
        } finally {
        getLogger().info("mineTwit goes tweet tweet");
        }
      return true;     
    } else {
      getLogger().info("Gibberish or a typo, either way it ain't happening");
      return false; 
        }
  }
    
 private void initialiseNotifications() {
  //TODO Incorporate this into config settings so can be saved.
  for (int i=0; i<myNotifications.length; i++) {
    myNotifications[i]= new notificationList(); 
  }
  // Set defaults
  myNotifications[0].type = "loggingInOut";
  myNotifications[0].status = true;
  //Set to false as can overload twitter update limits quickly with building
  myNotifications[1].type= "blockPlacing";
  myNotifications[1].status = false;
  myNotifications[2].type= "dying";
  myNotifications[2].status = true;
  myNotifications[3].type= "taming";
  myNotifications[3].status = true;
  myNotifications[4].type= "fishing";
  myNotifications[4].status = true;
  myNotifications[5].type= "kicking";
  myNotifications[5].status = true;
  myNotifications[6].type= "teleporting";
  myNotifications[6].status = true;
  myNotifications[7].type= "enteringVehicle"; 
  myNotifications[7].status = true;
  //Set to false as can overload twitter updates limits quickly with building
  myNotifications[8].type= "duplicate"; 
  myNotifications[8].status = false;
 }
  
  @EventHandler
  public void onBlockPlace(BlockPlaceEvent event) {
    if (myNotifications[1].status) {
      Player player = event.getPlayer();
      Block block = event.getBlock();
      Material mat = block.getType();
      // Tweet who placed which block.
      updateStatus(twitter, player.getName() + " placed a block of " + mat.toString().toLowerCase() + ".");
    } else {  
      return;
    } 
  }
  
  @EventHandler
  public void onDeath (final EntityDeathEvent event) {
    if (myNotifications[2].status) {
      if (!(event.getEntity() instanceof Player)) {
      updateStatus(twitter, "Something kicked the bucket.");
      } else {
        final Player player = (Player)event.getEntity();
        updateStatus(twitter, player.getName() + " kicked the bucket.");
      }
    } else {
      return;
    } 
  }
  
  @EventHandler
  public void onEntityTame (final EntityTameEvent event) {
    if (myNotifications[3].status) {
      final Player player = (Player)event.getOwner();
      final LivingEntity entity = (LivingEntity)event.getEntity();
      updateStatus(twitter, player.getName() + " tamed a " + entity.getCustomName());
    } else {
      return;
    }
  }
  
  @EventHandler
  public void onFishing (final PlayerFishEvent event) {
    if (myNotifications[4].status) {
        final Player player = (Player)event.getPlayer();
        updateStatus(twitter, player.getName() + " went fishing.");
    } else {
      return;
    }
  }
  
  @EventHandler
  public void onPlayerKick (final PlayerKickEvent event) {
    if (myNotifications[5].status) {
      final Player player = (Player)event.getPlayer();
      updateStatus(twitter, player.getName() + " was unceremoniously booted off.");
    } else {
      return;
    }
  }
  
  @EventHandler
  public void onPlayerTeleport (final PlayerTeleportEvent event) {
    if (myNotifications[6].status) {
      final Player player = (Player)event.getPlayer();
      final Location from = event.getFrom();
      final Location to = event.getTo();
      if (from.getBlockX() == to.getBlockX() && from.getBlockZ() == to.getBlockZ()) {
        return;
      }
      updateStatus(twitter, player.getName() + " teleported from X" + String.valueOf(from.getBlockX()) + ",Y" + String.valueOf(from.getBlockY()) + ",Z" + String.valueOf(from.getBlockZ())+ " to X" + String.valueOf(to.getBlockX()) + ",Y" + String.valueOf(to.getBlockY()) + ",Z" + String.valueOf(to.getBlockZ()));
    } else {
      return;
    }
  }
 
  @EventHandler
  public void onVehicleEnter (final VehicleEnterEvent event) {
    if (myNotifications[7].status) {
      if (!(event.getEntered() instanceof Player)) {
        return;
      }
      final Player player = (Player)event.getEntered();
      final Vehicle vehicle = event.getVehicle();
      updateStatus(twitter, player.getName() + " got into a " + String.valueOf(vehicle) + ".");
    } else {
      return;
    }
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
    // Check whether IP address is coming from router hence WAN
    if (recentPlayerIP.equals("192.168.1.1")) {
      return false;
    }
    // Otherwise it must be local
    else if (recentPlayerIP.startsWith("192.168")) {
      return true;
    }
    // Any other address is from outside
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
    } else {
      locationString = "X: " + playerLocation[0] + " Y: " + playerLocation[1] + " Z: " + playerLocation[2];
      getLogger().info(recentPlayer + " is not exempt from co-ord display");
    }
    return locationString;
  }
  
  private void copy(InputStream in, File file) {
    try {
        OutputStream out = new FileOutputStream(file);
        byte[] buf = new byte[1024];
        int len;
        while((len=in.read(buf))>0) {
            out.write(buf,0,len);
        }
        out.close();
        in.close();
    } catch (Exception e) {
      getLogger().info("[copy]Error loading default config file.");
        e.printStackTrace();
    }
}
  
  private Twitter setupTwitter(twitterSettings setupSettings) throws TwitterException {
    //if (TWITTER_CONFIGURED) {
    if (setupSettings.status) {
      TwitterFactory factory = new TwitterFactory();
      final Twitter twitter = factory.getInstance();
      AccessToken accessToken = loadAccessToken(setupSettings.token, setupSettings.secret);
      authenticateTwitter(accessToken, twitter, setupSettings.apiKey, setupSettings.apiSecret);
      getLogger().info("Twitter is enabled.");
      return twitter;
    } else {
      getLogger().info("Twitter is switched off you doughnut.");
      return null;
    }
  }
  
  private twitterSettings updateConfig(twitterSettings currentSettings) {
    twitterSettings updateSettings = new twitterSettings();
    //Load config from file to check values
    reloadConfig();
    //Boolean proposedStatus = this.getConfig().getBoolean("Twitter.TWITTER_CONFIGURED");
    String proposedStatus = this.getConfig().getString("Twitter.TWITTER_CONFIGURED");
    String proposedApiKey = this.getConfig().getString("Twitter.API_KEY");
    String proposedApiSecret = this.getConfig().getString("Twitter.API_SECRET");
    String proposedToken = this.getConfig().getString("Twitter.token");
    String proposedSecret = this.getConfig().getString("Twitter.secret");
    //Check if a valid status value is present otherwise revert to previous status
    if (proposedStatus.equalsIgnoreCase("false") || proposedStatus.equalsIgnoreCase("off") || proposedStatus.equalsIgnoreCase("no")) {
      updateSettings.status = false;
    } else if (proposedStatus.equalsIgnoreCase("true") || proposedStatus.equalsIgnoreCase("on") || proposedStatus.equalsIgnoreCase("yes")) {
      updateSettings.status = true;
    } else {
      updateSettings.status = currentSettings.status;       
    }
    updateSettings.apiKey = proposedApiKey;
    updateSettings.apiSecret = proposedApiSecret;
    updateSettings.token = proposedToken;
    updateSettings.secret = proposedSecret;   
    //Set values and save
    this.getConfig().set("Twitter.TWITTER_CONFIGURED", updateSettings.status);
    this.getConfig().set("Twitter.API_KEY", updateSettings.apiKey);
    this.getConfig().set("Twitter.API_SECRET", updateSettings.apiSecret);
    this.getConfig().set("Twitter.token", updateSettings.token);
    this.getConfig().set("Twitter.secret", updateSettings.secret); 
    saveConfig();
    return updateSettings;
  }
  
  //TODO Test handling of duplicates
  private void updateStatus(Twitter twitter, String newMessage) {
    if (twitter != null) {      
      SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm:ss z");
      Date now = new Date();
      //If rateLimited then check if reset time has passed and reset rateLimitStatus
      if (rateLimitStatus.limited) {
        //Get current time to check against    
        long currentTime = now.getTime()/1000L;
        //This line only used for debug code so could remove
        //Date currentDate = new java.util.Date(currentTime);
        getLogger().info("[DEBUG] Now is " + now);
        getLogger().info("[DEBUG] Current time is " + currentTime);
        getLogger().info("[DEBUG] rateLimitStatus.resetTime is " + rateLimitStatus.resetTime);
        //Limited use for debugging so remove
        //getLogger().info("[DEBUG] currentDate is " + sdf.format(currentDate));
        getLogger().info("[DEBUG] resetDate is " + rateLimitStatus.resetDate);
        if (rateLimitStatus.resetTime < currentTime) {
          getLogger().info("[DEBUG] Limit time passed so resetting");
          resetRateLimit(rateLimitStatus);
        }
      }
      // Check newMessage
      if(!rateLimitStatus.limited) {
        try {  
          String[] currentStatus = getCurrentStatus(twitter);
          if (!(newMessage.equals(currentStatus[0])) || !(String.valueOf(now).equals(currentStatus[1]))) {
            // Debug code to check twitter rate limits
            Map <String, RateLimitStatus> rateLimit = twitter.getRateLimitStatus();
            for (String endpoint : rateLimit.keySet()) {
              RateLimitStatus status = rateLimit.get(endpoint);
              //Omit any endpoints that haven't moved from default limit
              if (status.getRemaining() != status.getLimit()) {
                getLogger().info("[DEBUG] Endpoint: " + endpoint);
                getLogger().info("[DEBUG] Limit: " + status.getLimit());
                getLogger().info("[DEBUG] Remaining: " + status.getRemaining());
                Date endpointDate = new java.util.Date(status.getResetTimeInSeconds()*1000L);
                String formattedEndpointDate = sdf.format(endpointDate);
                getLogger().info("[DEBUG] Reset at: " + formattedEndpointDate);
                if(status.getRemaining()==0 &&(status.getResetTimeInSeconds() > rateLimitStatus.resetTime)) {
                  getLogger().info("[DEBUG] Rate limit hit for " + endpoint);
                  getLogger().info("[DEBUG] Old reset time " + rateLimitStatus.resetTime);
                  getLogger().info("[DEBUG] New reset time " + status.getResetTimeInSeconds());
                  rateLimitStatus.limited = true;
                  rateLimitStatus.endpointName = endpoint;
                  rateLimitStatus.resetTime = status.getResetTimeInSeconds();
                  rateLimitStatus.resetDate = formattedEndpointDate;
                }
              }
            }
            //Tweet if duplicates are on
            if (myNotifications[8].status) {
              getLogger().info("[DEBUG] Duplicates are true. Tweeting whatever the new message is.");
              twitter.updateStatus(newMessage + "\n" + now);
              //Tweet if duplicates are off but messages do not match
            } else if (!myNotifications[8].status && !newMessage.equals(currentStatus[0])) {
              getLogger().info("[DEBUG] Duplicates are false but message has changed so tweeting anyway.");
              getLogger().info("[DEBUG]Latest is '" + newMessage + "'");
              getLogger().info("[DEBUG]Last was '" + currentStatus[0] + "'");
              twitter.updateStatus(newMessage + "\n" + now);
            } else {
              getLogger().info("[DEBUG] Duplicates are false and message is duplicate, not tweeting");
            }
          } else {
            getLogger().info("Ignore as exact duplicate of previous message so Twitter will reject");      
          }
       } catch (TwitterException e) {
         getLogger().info("Twitter is broken because of " + e);
         throw new RuntimeException(e);
       }   
    } else {
      getLogger().info("Twitter is rate limited, not tweeting");
      Date limitDate = new java.util.Date(rateLimitStatus.resetTime*1000L);
      String formattedLimitDate = sdf.format(limitDate);
      getLogger().info("Limit ends at " + formattedLimitDate);
    }
    }
  }
  
  //TODO Test this
  private String[] getCurrentStatus (Twitter twitter) throws TwitterException {
    // Gets last user tweet from timeline.
    ResponseList<Status> userTimeLine = twitter.getUserTimeline();
    //Split off first line.
    String timeLine = userTimeLine.get(0).getText();
    String[] splitTimeLine = timeLine.split("\r\n|\r|\n",2);
    //Remove after testing
    //String[] currentStatus = splitTimeLine[0];
    //String currentStatus[1] = splitTimeLine[1];
    return splitTimeLine;
  }
  
  // Replace with static method after dubugging
  //private static void authenticateTwitter(AccessToken accessToken, Twitter twitter, String loadKey, String loadSecret) {
  private void authenticateTwitter(AccessToken accessToken, Twitter twitter, String loadKey, String loadSecret) {
    twitter.setOAuthConsumer(loadKey, loadSecret);
    twitter.setOAuthAccessToken(accessToken);
  }
  
  //Replace with static method after dubugging
  //private static AccessToken loadAccessToken(String loadToken, String loadSecret) {
  private AccessToken loadAccessToken(String loadToken, String loadSecret) {
    String token = loadToken;
    String tokenSecret = loadSecret;
    return new AccessToken(token, tokenSecret);
  }
  
  private void resetRateLimit(rateLimits rateLimit) {
    rateLimit.limited = false;
    rateLimit.endpointName = null;
    rateLimit.resetTime = 0;
    rateLimit.resetDate = null;
  }

}
