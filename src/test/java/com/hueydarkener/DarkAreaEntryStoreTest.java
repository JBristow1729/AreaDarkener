package com.hueydarkener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.Test;

public class DarkAreaEntryStoreTest
{
	@Test
	public void startsEmptyWhenNoSavedJson()
	{
		TestStorage storage = new TestStorage(null);
		DarkAreaEntryStore store = new DarkAreaEntryStore(storage);

		assertTrue(store.getEntries().isEmpty());
	}

	@Test
	public void loadsValidSavedJson()
	{
		TestStorage storage = new TestStorage("[{\"name\":\"GWD\",\"regionIds\":[11345,11346],\"darkness\":60,\"enabled\":false}]");
		DarkAreaEntryStore store = new DarkAreaEntryStore(storage);

		List<DarkAreaEntry> entries = store.getEntries();

		assertEquals(1, entries.size());
		assertEquals("GWD", entries.get(0).getName());
		assertEquals(Arrays.asList(11345, 11346), entries.get(0).getRegionIds());
		assertEquals(60, entries.get(0).getDarkness());
		assertFalse(entries.get(0).isEnabled());
	}

	@Test
	public void malformedJsonFallsBackToEmptyEntries()
	{
		TestStorage storage = new TestStorage("not json");
		DarkAreaEntryStore store = new DarkAreaEntryStore(storage);

		assertTrue(store.getEntries().isEmpty());
	}

	@Test
	public void malformedEntryShapeFallsBackToEmptyEntries()
	{
		TestStorage storage = new TestStorage("[{\"name\":\"Bad\",\"darkness\":45,\"enabled\":true}]");
		DarkAreaEntryStore store = new DarkAreaEntryStore(storage);

		assertTrue(store.getEntries().isEmpty());
	}

	@Test
	public void clampsDarknessToZeroThroughOneHundred()
	{
		TestStorage storage = new TestStorage("[{\"name\":\"Low\",\"regionIds\":[1],\"darkness\":-10,\"enabled\":true},"
			+ "{\"name\":\"High\",\"regionIds\":[2],\"darkness\":250,\"enabled\":true}]");
		DarkAreaEntryStore store = new DarkAreaEntryStore(storage);

		assertEquals(0, store.getEntries().get(0).getDarkness());
		assertEquals(100, store.getEntries().get(1).getDarkness());
	}

	@Test
	public void removesDuplicateRegionsAcrossEntries()
	{
		TestStorage storage = new TestStorage("[{\"name\":\"First\",\"regionIds\":[10,11],\"darkness\":45,\"enabled\":true},"
			+ "{\"name\":\"Second\",\"regionIds\":[11,12],\"darkness\":50,\"enabled\":true}]");
		DarkAreaEntryStore store = new DarkAreaEntryStore(storage);

		assertEquals(Arrays.asList(10, 11), store.getEntries().get(0).getRegionIds());
		assertEquals(Collections.singletonList(12), store.getEntries().get(1).getRegionIds());
	}

	@Test
	public void findsEntryContainingRegion()
	{
		TestStorage storage = new TestStorage("[{\"name\":\"Vorkath\",\"regionIds\":[9023],\"darkness\":70,\"enabled\":true}]");
		DarkAreaEntryStore store = new DarkAreaEntryStore(storage);

		Optional<DarkAreaEntry> entry = store.findEntryContainingRegion(9023);

		assertTrue(entry.isPresent());
		assertEquals("Vorkath", entry.get().getName());
		assertEquals(70, store.findDarknessForRegion(9023).orElseThrow(AssertionError::new));
	}

	@Test
	public void createsIncrementingMyEntryNames()
	{
		TestStorage storage = new TestStorage("[{\"name\":\"My Entry 1\",\"regionIds\":[10],\"darkness\":45,\"enabled\":true}]");
		DarkAreaEntryStore store = new DarkAreaEntryStore(storage);

		DarkAreaEntry entry = store.createCurrentRegionEntry(20);

		assertEquals("My Entry 2", entry.getName());
		assertEquals(Collections.singletonList(20), entry.getRegionIds());
		assertEquals(50, entry.getDarkness());
		assertTrue(entry.isEnabled());
	}

	private static final class TestStorage implements DarkAreaEntryStore.Storage
	{
		private String value;

		private TestStorage(String value)
		{
			this.value = value;
		}

		@Override
		public String load()
		{
			return value;
		}

		@Override
		public void save(String value)
		{
			this.value = value;
		}
	}
}
