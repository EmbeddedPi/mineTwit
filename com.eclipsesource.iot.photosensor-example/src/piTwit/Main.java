/*******************************************************************************
 * Copyright (c) 2014 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 *    EmbeddedPi - Converted scope of usage to Minecraft server notification
 ******************************************************************************/
package piTwit;

import java.util.Date;

import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.Twitter;
import twitter4j.TwitterException;
import twitter4j.TwitterFactory;
import twitter4j.auth.AccessToken;

/*
import com.pi4j.io.gpio.GpioController;
import com.pi4j.io.gpio.GpioFactory;
import com.pi4j.io.gpio.GpioPinDigitalInput;
import com.pi4j.io.gpio.GpioPinDigitalOutput;
import com.pi4j.io.gpio.PinPullResistance;
import com.pi4j.io.gpio.RaspiPin;
import com.pi4j.io.gpio.event.GpioPinDigitalStateChangeEvent;
import com.pi4j.io.gpio.event.GpioPinListenerDigital;
*/

public class Main {

  private static final String LIGHTS_ON_MSG = "Jeannie smells a bit funny";
  private static final String LIGHTS_OFF_MSG = "Jeannie has no nose, how does she smell?";
  private static final boolean TWITTER_CONFIGURED = false;
  private static final String API_KEY = "XXXX";
  private static final String API_SECRET = "YYYY";
  private static final String token = "ZZZ";
  private static final String secret = "ABABAB";


  static LightStatus currentStatus;
  private static Twitter twitter;

  enum LightStatus {
    ON(LIGHTS_OFF_MSG), OFF(LIGHTS_ON_MSG);
    String status;

    private LightStatus(String status) {
      this.status = status;
    }
  }

  public static void main(String[] args) throws Exception {
    System.out.println("Starting the photosensor demo...");
    twitter = setupTwitter();
/*    
    final GpioController gpio = GpioFactory.getInstance();
    final GpioPinDigitalOutput led = gpio.provisionDigitalOutputPin(RaspiPin.GPIO_06);
    final GpioPinDigitalInput sensor = gpio.provisionDigitalInputPin(RaspiPin.GPIO_01, PinPullResistance.PULL_DOWN);
    configureSenssor(led, sensor);
 */
    System.out.println("Current status is ..." + currentStatus);
    updateStatus(twitter, LightStatus.ON);
    System.out.println("All done.....");
    sleep();
  }
  

/*
  private static void configureSenssor(final GpioPinDigitalOutput led, final GpioPinDigitalInput sensor) {
    sensor.addListener(new GpioPinListenerDigital() {
      @Override
      public void handleGpioPinDigitalStateChangeEvent(final GpioPinDigitalStateChangeEvent event) {
        handleSensorInput(led, event);
      }

      private void handleSensorInput(final GpioPinDigitalOutput led, final GpioPinDigitalStateChangeEvent event) {
        if (event.getState().isLow()) {
          notifyLightsOff(twitter, led);
        } else {
          notifyLightsOn(twitter, led);
        }
      }

      private void notifyLightsOn(final Twitter twitter, final GpioPinDigitalOutput led) {
        led.low();
        updateStatus(twitter, LightStatus.ON);
      }

      private void notifyLightsOff(final Twitter twitter, final GpioPinDigitalOutput led) {
        led.high();
        updateStatus(twitter, LightStatus.OFF);
      }

 }

);
  
}
*/
  private static void sleep() throws InterruptedException {
    for (;;) {
      Thread.sleep(500);
    }
  }

  private static Twitter setupTwitter() throws TwitterException {
    if (TWITTER_CONFIGURED) {
      System.out.println("Twitter is configured...");
      TwitterFactory factory = new TwitterFactory();
      final Twitter twitter = factory.getInstance();
      AccessToken accessToken = loadAccessToken();
      authenticateTwitter(accessToken, twitter);
      currentStatus = getCurrentStatus(twitter);
      return twitter;
    }
    System.out.println("Twitter ain't configured...");
    return null;
  }

  private static void updateStatus(Twitter twitter, LightStatus newStatus) {
    if (twitter != null && currentStatus != newStatus) {
      System.out.println("Running updateStatus oojimabar ..");
      try {
        twitter.updateStatus(newStatus.status + " : Test message sent from my PC : " + new Date());
        currentStatus = newStatus;
      } catch (TwitterException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private static LightStatus getCurrentStatus(Twitter twitter) throws TwitterException {
    System.out.println("Running getCurrentStatus whatchamacallit..");
    ResponseList<Status> homeTimeline = twitter.getHomeTimeline();
    
    String text = homeTimeline.get(0).getText();
    if (text.contains("on")) {
      return LightStatus.ON;
    }
    return LightStatus.OFF;
  }

  private static void authenticateTwitter(AccessToken accessToken, Twitter twitter) {
    System.out.println("Running authenticateTwitter doo dah..");
    twitter.setOAuthConsumer(API_KEY, API_SECRET);
    twitter.setOAuthAccessToken(accessToken);
  }

  private static AccessToken loadAccessToken() {
    System.out.println("Running loadAccessToken thingymajig..");
    String token = Main.token;
    String tokenSecret = secret;
    return new AccessToken(token, tokenSecret);
  }

}
