package com.hueydarkener;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import net.runelite.client.ui.PluginPanel;

final class AreaDarkenerPanel extends PluginPanel
{
	private static final Color SECTION_BACKGROUND = new Color(35, 35, 35);
	private static final Color CARD_BACKGROUND = new Color(43, 43, 43);

	private final DarkAreaEntryStore store;
	private final HueyDarkenerConfig config;
	private final Supplier<OptionalInt> currentRegionSupplier;
	private final Runnable changedCallback;

	AreaDarkenerPanel(
		DarkAreaEntryStore store,
		HueyDarkenerConfig config,
		Supplier<OptionalInt> currentRegionSupplier,
		Runnable changedCallback
	)
	{
		super(false);
		this.store = store;
		this.config = config;
		this.currentRegionSupplier = currentRegionSupplier;
		this.changedCallback = changedCallback;

		setLayout(new BorderLayout());
		rebuild();
	}

	void rebuild()
	{
		removeAll();

		JPanel content = new JPanel();
		content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
		content.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		content.add(sectionTitle("Areas"));
		for (DarkAreaPreset preset : DarkAreaPreset.getPresets())
		{
			content.add(presetRow(preset));
			content.add(Box.createVerticalStrut(4));
		}

		content.add(Box.createVerticalStrut(8));
		content.add(sectionTitle("Current region"));
		content.add(currentRegionPanel(null));

		content.add(Box.createVerticalStrut(8));
		content.add(sectionTitle("My Areas"));
		if (store.getEntries().isEmpty())
		{
			content.add(mutedLabel("No areas yet."));
		}
		for (DarkAreaEntry entry : store.getEntries())
		{
			content.add(entryCard(entry));
			content.add(Box.createVerticalStrut(8));
		}

		add(content, BorderLayout.NORTH);
		revalidate();
		repaint();
	}

	private JPanel presetRow(DarkAreaPreset preset)
	{
		JPanel row = rowPanel();
		JLabel label = new JLabel(preset.getName());
		JButton add = new JButton("Add");
		Optional<DarkAreaEntry> duplicate = firstDuplicate(preset.getRegionIds());
		if (duplicate.isPresent())
		{
			add.setEnabled(false);
			add.setToolTipText("Region already added to \"" + duplicate.get().getName() + "\"");
		}
		add.addActionListener(event ->
		{
			List<Integer> regionIds = missingRegionIds(preset.getRegionIds());
			if (!regionIds.isEmpty())
			{
				List<DarkAreaEntry> entries = new ArrayList<>(store.getEntries());
				entries.add(new DarkAreaEntry(preset.getName(), regionIds, config.defaultAreaStrength(), true));
				saveAndRefresh(entries);
			}
		});

		row.add(label, constraints(0, 0, 1.0));
		row.add(add, constraints(1, 0, 0.0));
		return row;
	}

	private JPanel currentRegionPanel(DarkAreaEntry targetEntry)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setBackground(SECTION_BACKGROUND);
		panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

		JButton add = new JButton(targetEntry == null ? "Add current region" : "+ current region");
		OptionalInt currentRegion = currentRegionSupplier.get();
		if (!currentRegion.isPresent())
		{
			add.setEnabled(false);
			panel.add(mutedLabel("Current region unavailable."));
		}
		else
		{
			Optional<DarkAreaEntry> duplicate = store.findEntryContainingRegion(currentRegion.getAsInt());
			if (duplicate.isPresent())
			{
				add.setEnabled(false);
				panel.add(mutedLabel("Region already added to \"" + duplicate.get().getName() + "\""));
			}
		}

		add.addActionListener(event ->
		{
			OptionalInt region = currentRegionSupplier.get();
			if (!region.isPresent())
			{
				return;
			}

			if (targetEntry == null)
			{
				List<DarkAreaEntry> entries = new ArrayList<>(store.getEntries());
				entries.add(store.createCurrentRegionEntry(region.getAsInt()));
				saveAndRefresh(entries);
			}
			else if (store.addCurrentRegionToEntry(targetEntry, region.getAsInt()))
			{
				changedCallback.run();
				rebuild();
			}
		});
		panel.add(add);
		return panel;
	}

	private JPanel entryCard(DarkAreaEntry entry)
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.setBackground(CARD_BACKGROUND);
		card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JTextField name = new JTextField(entry.getName());
		name.addActionListener(event -> saveName(entry, name.getText()));
		name.addFocusListener(new FocusAdapter()
		{
			@Override
			public void focusLost(FocusEvent event)
			{
				saveName(entry, name.getText());
			}
		});
		card.add(name);
		card.add(Box.createVerticalStrut(6));

		card.add(new JLabel("IDs"));
		for (Integer regionId : entry.getRegionIds())
		{
			card.add(regionRow(entry, regionId));
		}
		card.add(currentRegionPanel(entry));
		card.add(Box.createVerticalStrut(6));

		JPanel controls = rowPanel();
		JCheckBox enabled = new JCheckBox("Enabled", entry.isEnabled());
		enabled.addActionListener(event ->
		{
			entry.setEnabled(enabled.isSelected());
			saveAndRefresh(store.getEntries());
		});
		JSpinner darkness = new JSpinner(new SpinnerNumberModel(entry.getDarkness(), 0, 100, 1));
		darkness.addChangeListener(event ->
		{
			entry.setDarkness((Integer) darkness.getValue());
			saveAndRefresh(store.getEntries());
		});
		JButton remove = new JButton("-");
		remove.setToolTipText("Remove entry");
		remove.addActionListener(event ->
		{
			List<DarkAreaEntry> entries = new ArrayList<>(store.getEntries());
			entries.remove(entry);
			saveAndRefresh(entries);
		});

		controls.add(enabled, constraints(0, 0, 1.0));
		controls.add(new JLabel("Strength"), constraints(1, 0, 0.0));
		controls.add(darkness, constraints(2, 0, 0.0));
		controls.add(remove, constraints(3, 0, 0.0));
		card.add(controls);

		return card;
	}

	private JPanel regionRow(DarkAreaEntry entry, Integer regionId)
	{
		JPanel row = rowPanel();
		row.add(new JLabel(String.valueOf(regionId)), constraints(0, 0, 1.0));
		JButton remove = new JButton("-");
		remove.setToolTipText("Remove region ID");
		remove.addActionListener(event ->
		{
			List<Integer> ids = new ArrayList<>(entry.getRegionIds());
			ids.remove(regionId);
			entry.setRegionIds(ids);
			saveAndRefresh(store.getEntries());
		});
		row.add(remove, constraints(1, 0, 0.0));
		return row;
	}

	private void saveName(DarkAreaEntry entry, String value)
	{
		String name = value == null || value.trim().isEmpty() ? store.nextEntryName() : value.trim();
		if (!name.equals(entry.getName()))
		{
			entry.setName(name);
			saveAndRefresh(store.getEntries());
		}
	}

	private Optional<DarkAreaEntry> firstDuplicate(List<Integer> regionIds)
	{
		return regionIds.stream()
			.map(store::findEntryContainingRegion)
			.filter(Optional::isPresent)
			.map(Optional::get)
			.findFirst();
	}

	private List<Integer> missingRegionIds(List<Integer> regionIds)
	{
		List<Integer> missing = new ArrayList<>();
		for (Integer regionId : regionIds)
		{
			if (!store.findEntryContainingRegion(regionId).isPresent())
			{
				missing.add(regionId);
			}
		}
		return missing;
	}

	private void saveAndRefresh(List<DarkAreaEntry> entries)
	{
		store.saveEntries(entries);
		changedCallback.run();
		rebuild();
	}

	private static JLabel sectionTitle(String text)
	{
		JLabel label = new JLabel(text);
		label.setFont(label.getFont().deriveFont(Font.BOLD));
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private static JLabel mutedLabel(String text)
	{
		JLabel label = new JLabel(text);
		label.setForeground(Color.GRAY);
		label.setAlignmentX(Component.LEFT_ALIGNMENT);
		return label;
	}

	private static JPanel rowPanel()
	{
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
		return panel;
	}

	private static GridBagConstraints constraints(int x, int y, double weightX)
	{
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.weightx = weightX;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.insets = new Insets(1, 1, 1, 1);
		return constraints;
	}
}
