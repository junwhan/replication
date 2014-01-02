package edu.vt.rt.hyflow.core.dir.ballistic.strucure;

import java.util.LinkedList;
import java.util.List;

public class Structure
{

	private List<List<Integer>> leaders;
	private int[][] matrix;

	public Structure(List<List<Integer>> leaders, int[][] matrix)
	{
		this.leaders = leaders;
		this.matrix = matrix;
	}

	// We create the structure table of Ballistic
	public List<List<Integer>> getStructure(int lookup_distance,
			int move_distance, int node, int level)
	{

		int power = 1;
		for (int i = 1; i < (level + 1); i++)
		{
			power = power * 2;
		}
		lookup_distance = power * lookup_distance;
		move_distance = power * move_distance;

		List<List<Integer>> node_car = new LinkedList<List<Integer>>();
		if (level == leaders.size())
		{

			return null;
		}

		List<Integer> nodeLeaders = leaders.get(level);

		// Lookup parent
		LinkedList<Integer> lookup_parent = new LinkedList<Integer>();

		// We set up the distance
		node_car.add(lookup_parent);
		for (Integer leader : nodeLeaders)
		{
			int dist = matrix[leader - 1][node - 1];
			if (dist < lookup_distance)
				lookup_parent.add(leader);
		}

		// Move parent
		LinkedList<Integer> move_parent = new LinkedList<Integer>();

		node_car.add(move_parent);
		for (Integer leader : nodeLeaders)
		{
			int dist = matrix[leader - 1][node - 1];
			if (dist < move_distance)
				move_parent.add(leader);
		}

		// Home parent
		LinkedList<Integer> home_parent = new LinkedList<Integer>();
		node_car.add(home_parent);
		int min = Integer.MAX_VALUE;
		Integer homeParent = -1;
		for (Integer leader : nodeLeaders)
		{
			int dist = matrix[leader - 1][node - 1];
			if (dist < min)
			{
				min = dist;
				homeParent = leader;
			}
		}
		home_parent.add(homeParent);

		return node_car;
	}
}
