/*
 * Copyright (c) 2017, Devin French <https://github.com/devinfrench>
 * Copyright (c) 2019, Jordan Atwood <nightfirecat@protonmail.com>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package net.runelite.client.plugins.miningbot;

import com.google.inject.Provides;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import javax.inject.Inject;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.Perspective;
import net.runelite.api.Skill;
import net.runelite.api.WallObject;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.GameObjectChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.WallObjectDespawned;
import net.runelite.api.events.WallObjectSpawned;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.ImageUtil;

@PluginDescriptor(
	name = "Mining bot",
	description = "Bot that mines shit"
)
public class MiningBotPlugin extends Plugin
{
	@Inject
	private Client client;

	@Inject
	private ClientThread clientThread;

	@Inject
	private MiningBotConfig config;

	@Inject
	private MiningBotOverlay overlay;

	@Inject
	private OverlayManager overlayManager;

	@Inject
	private ClientToolbar clientToolbar;

	@Getter
	public List<GameObject> banks = new ArrayList<>();
	public List<GameObject> portal = new ArrayList<>();
	public List<WallObject> mort_myre_door = new ArrayList<>();
	public List<GameObject> fungi = new ArrayList<>();

	private MiningBotPanel panel;
	private NavigationButton navButton;
	Random rand = new Random();

	public WorldPoint bloom_spot = new WorldPoint(3436, 3453, 0);
	public WorldPoint next_to_bloom_spot = new WorldPoint(3437, 3453, 0);
	public WorldPoint cw_walk_spot = new WorldPoint(3378, 3164, 0);
	public WorldPoint cw_bank_spot = new WorldPoint(3360, 3168, 0);
	public WorldPoint cw_portal_tele = new WorldPoint(3327, 4751, 0);

	public Instant startTime = Instant.now();
	private Instant waitTime = Instant.now();
	private int timeToWait = 0;
	private boolean moving = false;
	protected boolean started = false;
	private int x0,x1,x2,x3,y0,y1,y2,y3,currentIteration = 0;
	private int amountOfIterations = 1;
	public String state = "open gate";
	private boolean hover = false;

	private boolean clickedGate = false;
	private boolean clickedLogsSpot = false;


	@Provides
    MiningBotConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(MiningBotConfig.class);
	}

	@Subscribe
	public void onConfigChanged(ConfigChanged event)
	{

	}

	@Override
	protected void startUp() throws Exception
	{
		overlayManager.add(overlay);

		panel = injector.getInstance(MiningBotPanel.class);

		final BufferedImage icon = ImageUtil.getResourceStreamFromClass(getClass(), "panel_icon.png");

		navButton = NavigationButton.builder()
				.tooltip("Fungus bot")
				.icon(icon)
				.priority(1)
				.panel(panel)
				.build();

		clientToolbar.addNavigation(navButton);
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
	}

	@Schedule( period = 10, unit = ChronoUnit.MILLIS )
	public void mainLoop()
	{
		if (!started)
		{
			return;
		}
		if(moving)
		{
			try {
				Robot robot = new Robot();
				if(currentIteration >= amountOfIterations)
				{
					if (!hover)
					{
						robot.mouseMove(x3,y3);
						robot.mousePress(InputEvent.BUTTON1_MASK);
						robot.mouseRelease(InputEvent.BUTTON1_MASK);
					}
					moving = false;
				}
				else
				{
					robot.mouseMove(BezierX(), BezierY());
					currentIteration++;
				}
			} catch (AWTException e) {
				e.printStackTrace();
			}
		}
		else
		{
			panel.stateLabel.setText(state);
			if (!compareTime(waitTime, timeToWait)) {return;}
			switch (state)
			{
				case "idle":
					break;
				case "open gate":
					if (!clickedGate)
					{
						if (mort_myre_door.size() > 0)
						{
							Rectangle2D clickbox = mort_myre_door.get(0).getClickbox().getBounds2D();
							startMouseMove((int)clickbox.getCenterX(), (int)clickbox.getCenterY(), false);
							startTime = Instant.now();
							clickedGate = true;
						}
					}
					else
					{
						if (compareTime(startTime, 5000))
						{
							clickedGate = false;
						}
					}
					break;
				case "walk to logs":
					if (client.getLocalPlayer().getWorldLocation().equals(bloom_spot))
					{
						startMouseMove((int)client.getWidget(548, 52).getBounds().getCenterX() + rand.nextInt(9) - 4, (int)client.getWidget(548, 52).getBounds().getCenterY() + rand.nextInt(9) - 4, false);
						state = "bloom";
						break;
					}
					if (!clickedLogsSpot)
					{
						if (bloom_spot.isInScene(client))
						{
							startMouseMove(Perspective.localToCanvas(client, LocalPoint.fromWorld(client, bloom_spot), bloom_spot.getPlane(), 0).getX(), Perspective.localToCanvas(client, LocalPoint.fromWorld(client, bloom_spot), bloom_spot.getPlane(), 0).getY(), false);
							startTime = Instant.now();
							clickedLogsSpot = true;
						}
					}
					else
					{
						if (compareTime(startTime, 5000))
						{
							clickedLogsSpot = false;
						}
					}
					break;
				case "bloom":
					if (client.getBoostedSkillLevel(Skill.PRAYER) == 0)
					{
						state = "idle";
						break;
					}
					startMouseMove((int)client.getWidget(548, 52).getBounds().getCenterX() + rand.nextInt(9) - 94, (int)client.getWidget(548, 52).getBounds().getCenterY() + rand.nextInt(9) + 107, true);
					setWait(200 + rand.nextInt(100));
					state = "right click sickle";
					break;
				case "right click sickle":
					try
					{
						Robot robot = new Robot();
						robot.mouseMove(x3,y3);
						robot.mousePress(InputEvent.BUTTON3_MASK);
						robot.mouseRelease(InputEvent.BUTTON3_MASK);
					}
					catch (AWTException e)
					{
						e.printStackTrace();
					}
					startMouseMove((int)MouseInfo.getPointerInfo().getLocation().getX() - 5, (int)MouseInfo.getPointerInfo().getLocation().getY() + 8 + rand.nextInt(4), false);
					state = "hover over log";
					break;
				case "hover over log":
					setWait(1500 + rand.nextInt(200));
					startMouseMove(Perspective.localToCanvas(client, LocalPoint.fromWorld(client, next_to_bloom_spot), next_to_bloom_spot.getPlane(), 0).getX(), Perspective.localToCanvas(client, LocalPoint.fromWorld(client, next_to_bloom_spot), next_to_bloom_spot.getPlane(), 0).getY(), true);
					state = "wait till bloom";
					break;
				case "wait till bloom":
					System.out.println(fungi.size());
					if (fungi.size() > 0)
					{
						state = "pick fungi";
					}
					break;
				case "pick fungi":
					if (fungi.size() > 0)
					{
						Rectangle2D clickbox = fungi.get(0).getClickbox().getBounds2D();
						startMouseMove((int)clickbox.getCenterX(), (int)clickbox.getCenterY(), false);
						setWait(1000 + rand.nextInt(200));
					}
					else
					{
						state = "bloom";
					}
					break;
			}
		}
	}

	private int BezierX()
	{
		double s = ((double)currentIteration)/amountOfIterations;
		double t = Math.pow(s,0.8);
		//System.out.println(currentIteration + " " + amountOfIterations + " " + t + " " + s);
		//System.out.println((int)(Math.pow((1 - t), 3)*x0+(3*Math.pow((1 - t), 2))*t*x1+(3*(1 - t))*Math.pow(t,2)*x2+Math.pow(t,3)*x3));
		return (int)(Math.pow((1 - t), 3)*x0+(3*Math.pow((1 - t), 2))*t*x1+(3*(1 - t))*Math.pow(t,2)*x2+Math.pow(t,3)*x3);
	}

	private int BezierY()
	{
		double s = ((double)currentIteration)/amountOfIterations;
		double t = Math.pow(s,0.8);
		//System.out.println(currentIteration + " " + amountOfIterations + " " + t + " " + s);
		//System.out.println((int)(Math.pow((1 - t), 3)*y0+(3*Math.pow((1 - t), 2))*t*y1+(3*(1 - t))*Math.pow(t,2)*y2+Math.pow(t,3)*y3));
		return (int)(Math.pow((1 - t), 3)*y0+(3*Math.pow((1 - t), 2))*t*y1+(3*(1 - t))*Math.pow(t,2)*y2+Math.pow(t,3)*y3);
	}

	private void startMouseMove(int xend, int yend, boolean doHover)
	{
		hover = doHover;
		moving = true;
		x0 = (int)MouseInfo.getPointerInfo().getLocation().getX();
		y0 = (int)MouseInfo.getPointerInfo().getLocation().getY();
		x3 = xend + 5;
		y3 = yend + 28;
		x1 = (int)((x0+(xend-x0)/3)+(Math.random()*(Math.abs(xend-x0)/2+2)-(Math.abs(xend-x0)/2+2)/2));
		x2 = (int)((x0+(xend-x0)/1.5)+(Math.random()*(Math.abs(xend-x0)/2+2)-(Math.abs(xend-x0)/2+2)/2));
		y1 = (int)((y0+(yend-y0)/3)+(Math.random()*(Math.abs(yend-y0)/2+2)-(Math.abs(yend-y0)/2+2)/2));
		y2 = (int)((y0+(yend-y0)/1.5)+(Math.random()*(Math.abs(yend-y0)/2+2)-(Math.abs(yend-y0)/2+2)/2));
		currentIteration = 0;
		amountOfIterations = (int)(7+Math.abs(Math.sqrt(Math.pow((xend-x0),2)+Math.pow((yend-y0),2)))/15);
		System.out.println("moving mouse to (" + xend + ", " + y3 + ") in " + amountOfIterations + " steps (" + ((double)amountOfIterations/50) + " seconds)");
	}

	private boolean compareTime(Instant timeStarted, int duration)
	{
		return Instant.now().toEpochMilli() - timeStarted.toEpochMilli() > duration;
	}

	private void setWait(int waitTime)
	{
		timeToWait = waitTime;
		this.waitTime = Instant.now();
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if(event.getGameObject().getId() == 26645)
		{
			portal.add(event.getGameObject());
		}
		if(event.getGameObject().getId() == 26707)
		{
			banks.add(event.getGameObject());
		}
		if(event.getGameObject().getId() == 3509)
		{
			fungi.add(event.getGameObject());
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		if(event.getGameObject().getId() == 26645)
		{
			portal.remove(event.getGameObject());
		}
		if(event.getGameObject().getId() == 26707)
		{
			banks.remove(event.getGameObject());
		}
		if(event.getGameObject().getId() == 3509)
		{
			fungi.remove(event.getGameObject());
		}
	}

	@Subscribe
	public void onGameObjectChanged(GameObjectChanged event)
	{
		if (event.getGameObject().getId() == 3509)
		{
			fungi.add(event.getGameObject());
			System.out.println("added fungus");
		}
		if (event.getPrevious().getId() == 3509)
		{
			fungi.remove(event.getPrevious());
			System.out.println("removed fungus");
		}
	}

	@Subscribe
	public void onWallObjectDespawned(WallObjectDespawned event)
	{
		if(event.getWallObject().getId() == 3507)
		{
			mort_myre_door.remove(event.getWallObject());
		}
	}

	@Subscribe
	public void onWallObjectSpawned(WallObjectSpawned event)
	{
		if(event.getWallObject().getId() == 3507)
		{
			mort_myre_door.add(event.getWallObject());
		}
	}

	@Subscribe
	public void onGameStateChanged(GameStateChanged event)
	{
		if (event.getGameState() == GameState.LOADING)
		{
			mort_myre_door.clear();
			banks.clear();
			portal.clear();
			fungi.clear();
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage event)
	{
		if (event.getMessage().contains("walk into the gloomy atmosphere"))
		{
			state = "walk to logs";
			setWait(rand.nextInt(200) + 800);
			clickedGate = false;
		}
	}
}
