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
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;

import lombok.Getter;
import net.runelite.api.Client;
import net.runelite.api.GameObject;
import net.runelite.api.events.ConfigChanged;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.task.Schedule;
import net.runelite.client.ui.overlay.OverlayManager;

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

	@Getter
	public List<GameObject> banks = new ArrayList<>();

	private int count = 0;
	private boolean moving = false;
	private int x0,x1,x2,x3,y0,y1,y2,y3,currentIteration = 0;
	private int amountOfIterations = 1;

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
		startMouseMove(550, 300);
		moving = true;
	}

	@Override
	protected void shutDown() throws Exception
	{
		overlayManager.remove(overlay);
	}

	@Schedule( period = 10, unit = ChronoUnit.MILLIS )
	public void mainLoop()
	{
		if(moving)
		{
			try {
				Robot robot = new Robot();
				if(currentIteration >= amountOfIterations)
				{
					robot.mouseMove(x3,y3);
					robot.mousePress(InputEvent.BUTTON1_MASK);
					robot.mouseRelease(InputEvent.BUTTON1_MASK);
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

	private void startMouseMove(int xend, int yend)
	{
		moving = true;
		x0 = (int)MouseInfo.getPointerInfo().getLocation().getX();
		y0 = (int)MouseInfo.getPointerInfo().getLocation().getY();
		x3 = xend;
		y3 = yend+58;
		x1 = (int)((x0+(xend-x0)/3)+(Math.random()*(Math.abs(xend-x0)/2+2)-(Math.abs(xend-x0)/2+2)/2));
		x2 = (int)((x0+(xend-x0)/1.5)+(Math.random()*(Math.abs(xend-x0)/2+2)-(Math.abs(xend-x0)/2+2)/2));
		y1 = (int)((y0+(yend-y0)/3)+(Math.random()*(Math.abs(yend-y0)/2+2)-(Math.abs(yend-y0)/2+2)/2));
		y2 = (int)((y0+(yend-y0)/1.5)+(Math.random()*(Math.abs(yend-y0)/2+2)-(Math.abs(yend-y0)/2+2)/2));
		currentIteration = 0;
		amountOfIterations = (int)(7+Math.abs(Math.sqrt(Math.pow((xend-x0),2)+Math.pow((yend-y0),2)))/15);
		System.out.println("moving mouse to (" + xend + ", " + yend + ") in " + amountOfIterations + " steps (" + ((double)amountOfIterations/50) + " seconds)");
	}

	@Subscribe
	public void onGameObjectSpawned(GameObjectSpawned event)
	{
		if(event.getGameObject().getId() == 26707)
		{
			banks.add(event.getGameObject());
		}
	}

	@Subscribe
	public void onGameObjectDespawned(GameObjectDespawned event)
	{
		if(event.getGameObject().getId() == 26707)
		{
			banks.remove(event.getGameObject());
		}
	}
}
