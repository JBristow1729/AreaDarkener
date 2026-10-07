package com.areadarkener;

import java.util.ArrayList;
import java.util.List;

/** Shared ground edges between tiles resolving to different region IDs. */
final class RegionBoundaries
{
	static List<Edge> find(int[][] regions)
	{
		List<Edge> edges = new ArrayList<>();
		for (int x = 0; x < regions.length; x++)
		{
			for (int y = 0; y < regions[x].length; y++)
			{
				int region = regions[x][y];
				if (region < 0)
				{
					continue;
				}
				if (x + 1 < regions.length && y < regions[x + 1].length
					&& regions[x + 1][y] >= 0 && regions[x + 1][y] != region)
				{
					edges.add(new Edge(x + 1, y, x + 1, y + 1));
				}
				if (y + 1 < regions[x].length && regions[x][y + 1] >= 0 && regions[x][y + 1] != region)
				{
					edges.add(new Edge(x, y + 1, x + 1, y + 1));
				}
			}
		}
		return edges;
	}

	static final class Edge
	{
		final int x1;
		final int y1;
		final int x2;
		final int y2;

		Edge(int x1, int y1, int x2, int y2)
		{
			this.x1 = x1;
			this.y1 = y1;
			this.x2 = x2;
			this.y2 = y2;
		}
	}
}
