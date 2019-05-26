/*
 * Copyright (c) 2017, Kronos <https://github.com/KronosDesign>
 * Copyright (c) 2017, Adam <Adam@sigterm.info>
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

import net.runelite.api.Client;
import net.runelite.client.ui.ColorScheme;
import net.runelite.client.ui.FontManager;
import net.runelite.client.ui.PluginPanel;

import javax.inject.Inject;
import javax.swing.*;
import java.awt.*;

class MiningBotPanel extends PluginPanel
{
	private final Client client;
	private final MiningBotPlugin plugin;

	public JLabel title = new JLabel("Fungus picker bot");
	public JButton start_bot = new JButton("Start bot");
	public JLabel stateText = new JLabel("Current task:");
	public JLabel stateLabel = new JLabel("");

	@Inject
	private MiningBotPanel(Client client, MiningBotPlugin plugin)
	{
		super();

		this.client = client;
		this.plugin = plugin;

		setBackground(ColorScheme.DARK_GRAY_COLOR);

		add(createOptionsPanel());
	}

	private JPanel createOptionsPanel()
	{
		final JPanel container = new JPanel();
		container.setBackground(ColorScheme.DARK_GRAY_COLOR);
		container.setLayout(new BoxLayout(container, BoxLayout.PAGE_AXIS));
        container.add(Box.createRigidArea(new Dimension(0, 20)));
		title.setAlignmentX(0.5f);
		title.setFont(FontManager.getRunescapeBoldFont());
        container.add(title);
        container.add(Box.createRigidArea(new Dimension(0, 10)));
        start_bot.setAlignmentX(0.5f);
        start_bot.addActionListener((ev) ->
        {
            plugin.started = !plugin.started;
            if (plugin.started)
            {
                start_bot.setText("Stop bot");
            }
            else
            {
                start_bot.setText("Start bot");
            }
            start_bot.setFocusPainted(false);
        });
		container.add(start_bot);
		stateLabel.setAlignmentX(0.5f);
		container.add(Box.createRigidArea(new Dimension(0, 15)));
		stateText.setAlignmentX(0.5f);
		container.add(stateText);
		container.add(Box.createRigidArea(new Dimension(0, 5)));
		container.add(stateLabel);
		return container;
	}
}
