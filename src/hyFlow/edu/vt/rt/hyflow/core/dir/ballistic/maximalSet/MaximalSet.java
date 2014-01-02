package edu.vt.rt.hyflow.core.dir.ballistic.maximalSet;

import java.util.LinkedList;
import java.util.List;

public class MaximalSet
{
	private int[][] matrix;

	public MaximalSet(int[][] matrix)
	{
		this.matrix = matrix;
	}

	public List<Integer> getMaximalSet(int weight)
	{
		int[][] mat = new int[matrix.length][matrix.length];
		for (int i = 0; i < matrix[0].length; i++)
		{
			for (int j = 0; j < matrix[0].length; j++)
			{
				if (matrix[i][j] == weight)
					mat[i][j] = matrix[i][j];
				else
					mat[i][j] = 0;
			}
		}

		// Maximum independent set algorithm

		// We first find the degree of each vertex
		LinkedList<Integer> MIS = new LinkedList<Integer>();
		LinkedList<Integer> S = new LinkedList<Integer>();

		int lent = 0;

		for (int i = 0; i < matrix[0].length; i++)
		{
			if (matrix[i][i] != -1)
				lent++;
		}

		int[] V = new int[lent];
		int o = 0;
		for (int m = 0; m < matrix[0].length; m++)
		{
			if (matrix[m][m] != -1)
			{
				V[o] = (m + 1);
				o++;
			}
		}

		// We first find the degree of each vertex
		int[] degree = new int[lent];
		int n = 0;
		for (int i = 0; i < V.length; i++)
		{
			n = 0;
			for (int j = 0; j < V.length; j++)
			{
				if (mat[V[i] - 1][V[j] - 1] != 0)
					n++;
			}
			degree[i] = n;
		}

		int v_empty = 0;
		int i, j, k;
		int degree_value = 0;
		int a = 0, b = 0;

		boolean check = false;
		while (check == false)
		{
			for (i = 0; i < degree.length; i++)
			{
				if ((degree[i] == degree_value) && (V[i] != 0))
				{
					S.add(V[i]);
				}
			}
			for (i = 0; i < mat[0].length; i++)
			{
				for (j = 0; j < mat[0].length; j++)
				{
					if (mat[i][j] != 0)
					{
						if ((S.contains(i + 1) == true)
								&& (S.contains(j + 1) == true))
						{
							for (int z = 0; z < V.length; z++)
							{
								if (V[z] == i + 1)
									a = z;
								if (V[z] == j + 1)
									b = z;
							}
							if (degree[a] >= degree[b])
							{
								for (k = 0; k < S.size(); k++)
								{
									if (S.get(k) == j + 1)
										S.remove(k);
								}
							}
						}
					}
				}
			}
			for (i = 0; i < S.size(); i++)
			{
				k = S.get(i);
				MIS.add(k);
				for (int z = 0; z < V.length; z++)
				{
					if (V[z] == k)
						V[z] = 0;
				}
				for (j = 0; j < mat[0].length; j++)
				{
					if (mat[k - 1][j] != 0)
					{
						for (int z = 0; z < V.length; z++)
						{
							if (V[z] == j + 1)
								V[z] = 0;
						}
						mat[k - 1][j] = 0;
					}
				}
			}
			S.clear();
			degree_value++;
			// end of algortithm condition
			v_empty = 0;
			for (i = 0; i < V.length; i++)
			{
				if (V[i] == 0)
					v_empty++;
			}
			if (v_empty == V.length)
				check = true;
		}
		return MIS;
	}
}
