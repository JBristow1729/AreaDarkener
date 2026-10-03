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
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;
import net.runelite.client.ui.PluginPanel;

final class AreaDarkenerPanel extends PluginPanel
{
	private static final Color SECTION_BACKGROUND = new Color(35, 35, 35);
	private static final Color CARD_BACKGROUND = new Color(43, 43, 43);

	private final DarkAreaEntryStore store;
	private final Supplier<OptionalInt> currentRegionSupplier;
	private final Runnable changedCallback;

	AreaDarkenerPanel(
		DarkAreaEntryStore store,
		Supplier<OptionalInt> currentRegionSupplier,
		Runnable changedCallback
	)
	{
		super(false);
		this.store = store;
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

		content.add(sectionTitle("Presets"));
		content.add(presetsPanel());

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

	private JPanel presetsPanel()
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);

		JComboBox<DarkAreaPreset> presets = new JComboBox<>(DarkAreaPreset.getPresets().toArray(new DarkAreaPreset[0]));
		JButton add = new JButton("Add");
		add.setAlignmentX(Component.CENTER_ALIGNMENT);
		updatePresetAddButton(add, (DarkAreaPreset) presets.getSelectedItem());
		presets.addActionListener(event -> updatePresetAddButton(add, (DarkAreaPreset) presets.getSelectedItem()));
		add.addActionListener(event ->
		{
			DarkAreaPreset preset = (DarkAreaPreset) presets.getSelectedItem();
			if (preset == null)
			{
				return;
			}

			List<Integer> regionIds = missingRegionIds(preset.getRegionIds());
			if (!regionIds.isEmpty())
			{
				List<DarkAreaEntry> entries = new ArrayList<>(store.getEntries());
				entries.add(new DarkAreaEntry(preset.getName(), regionIds, DarkAreaEntryStore.DEFAULT_AREA_STRENGTH, true));
				saveAndRefresh(entries);
			}
		});

		panel.add(presets);
		panel.add(Box.createVerticalStrut(4));
		panel.add(add);
		return panel;
	}

	private void updatePresetAddButton(JButton add, DarkAreaPreset preset)
	{
		Optional<DarkAreaEntry> duplicate = preset == null ? Optional.empty() : firstDuplicate(preset.getRegionIds());
		add.setEnabled(!duplicate.isPresent());
		add.setToolTipText(duplicate.map(entry -> "Region already added to \"" + entry.getName() + "\"").orElse(null));
	}

	private JPanel currentRegionPanel(DarkAreaEntry targetEntry)
	{
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
		panel.setAlignmentX(Component.LEFT_ALIGNMENT);
		panel.setBackground(SECTION_BACKGROUND);
		panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

		JButton add = new JButton(targetEntry == null ? "Add current region" : "+ current region");
		add.setAlignmentX(Component.CENTER_ALIGNMENT);
		JLabel helper = null;
		OptionalInt currentRegion = currentRegionSupplier.get();
		if (!currentRegion.isPresent())
		{
			add.setEnabled(false);
			helper = mutedLabel("Current region unavailable.");
		}
		else
		{
			Optional<DarkAreaEntry> duplicate = store.findEntryContainingRegion(currentRegion.getAsInt());
			if (duplicate.isPresent())
			{
				add.setEnabled(false);
				helper = mutedLabel("Region already added to \"" + duplicate.get().getName() + "\"");
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
		if (targetEntry == null && helper != null)
		{
			helper.setAlignmentX(Component.CENTER_ALIGNMENT);
			panel.add(Box.createVerticalStrut(4));
			panel.add(helper);
		}
		return panel;
	}

	private JPanel entryCard(DarkAreaEntry entry)
	{
		JPanel card = new JPanel();
		card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
		card.setAlignmentX(Component.LEFT_ALIGNMENT);
		card.setBackground(CARD_BACKGROUND);
		card.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel header = rowPanel();
		JCheckBox enabled = new JCheckBox("", entry.isEnabled());
		enabled.setToolTipText("Enable area");
		enabled.addActionListener(event ->
		{
			entry.setEnabled(enabled.isSelected());
			saveAndRefresh(store.getEntries());
		});
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
		JButton remove = new JButton("\uD83D\uDDD1");
		remove.setToolTipText("Remove entry");
		remove.addActionListener(event ->
		{
			List<DarkAreaEntry> entries = new ArrayList<>(store.getEntries());
			entries.remove(entry);
			saveAndRefresh(entries);
		});
		header.add(enabled, constraints(0, 0, 0.0));
		header.add(name, constraints(1, 0, 1.0));
		header.add(remove, constraints(2, 0, 0.0));
		card.add(header);
		card.add(Box.createVerticalStrut(6));

		card.add(new JLabel("IDs"));
		for (Integer regionId : entry.getRegionIds())
		{
			card.add(regionRow(entry, regionId));
		}
		card.add(currentRegionPanel(entry));
		card.add(Box.createVerticalStrut(6));

		JPanel controls = rowPanel();
		JSpinner darkness = new JSpinner(new SpinnerNumberModel(entry.getDarkness(), 0, 100, 1));
		darkness.addChangeListener(event ->
		{
			entry.setDarkness((Integer) darkness.getValue());
			saveAndRefresh(store.getEntries());
		});

		controls.add(new JLabel("Strength"), constraints(0, 0, 1.0));
		controls.add(darkness, constraints(1, 0, 0.0));
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
		label.setHorizontalAlignment(SwingConstants.CENTER);
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
