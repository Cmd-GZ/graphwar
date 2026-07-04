//  Copyright (C) 2011 Lucas Catabriga Rocha <catabriga90@gmail.com>
//
//  This file is part of Graphwar.
//
//  Graphwar is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//
//  Graphwar is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.

//  You should have received a copy of the GNU General Public License
//  along with Graphwar.  If not, see <http://www.gnu.org/licenses/>.
package Graphwar;

import GraphServer.Constants;

public class Function
{
	private String strFunc;
	private PolishNotationFunction polishFunc;
	private double offSet;

	private double fireAngle;
	private double[] valuesX;
	private double[] valuesY;
	private double[] valuesDY;
	private int numSteps;

	private int[] playersHit;
	private int[] soldiersHit;
	private int[] soldierHitPosition;
	private int numPlayersHit;

	private double lastX;
	private double lastY;

	private void init()
	{
		offSet = 0;

		fireAngle = 0;
		valuesX = null;
		valuesY = null;
		valuesDY = null;
		numSteps = 0;

		playersHit = null;
		soldiersHit = null;
		soldierHitPosition = null;
		numPlayersHit = 0;

		lastX = 0;
		lastY = 0;
	}

	Function(String str) throws MalformedFunction
	{
		strFunc = str;
		polishFunc = new PolishNotationFunction(strFunc);

		init();

	}

	Function(PolishNotationFunction polishNotationFunction)
	{
		strFunc = "";
		polishFunc = polishNotationFunction;

		init();

	}

	public int getNumPlayersHit()
	{
		return numPlayersHit;
	}

	public int getPlayerHit(int index)
	{
		return playersHit[index];
	}

	public int getSoldierHit(int index)
	{
		return soldiersHit[index];
	}

	public int getSoldierHitPosition(int index)
	{
		return soldierHitPosition[index];
	}

	public double getFireAngle()
	{
		return fireAngle;
	}

	public double getX(int index)
	{
		return valuesX[index];
	}

	public double getY(int index)
	{
		return valuesY[index];
	}

	public int getNumSteps()
	{
		return numSteps;
	}

	public String getStringFunc()
	{
		return strFunc;
	}

	private double getStartAngle(double x, double radius)
	{
		double angle = 0;

		double startAngleTangent = (polishFunc.evaluateFunction(x+Constants.STEP_SIZE,0,0) - polishFunc.evaluateFunction(x,0,0))/Constants.STEP_SIZE;
		angle = Math.atan(startAngleTangent);

		double finalX;
		double error = 10000;
		for(int i=0; error > Constants.ANGLE_ERROR && i<Constants.MAX_ANGLE_LOOPS; i++)
		{
			finalX = x + radius*Math.cos(angle);
			startAngleTangent = (polishFunc.evaluateFunction(finalX+Constants.STEP_SIZE,0,0) - polishFunc.evaluateFunction(finalX,0,0))/Constants.STEP_SIZE;

			double newAngle = Math.atan(startAngleTangent);

			error = Math.abs(newAngle - angle);

			//System.out.println(angle+" "+newAngle);
			//System.out.println(x+" "+finalX+" "+radius+"\n");

			angle = newAngle;


		}

		return angle;
	}

	private double getRK4(double x, double y, double step)
	{
		double k1 = polishFunc.evaluateFunction( x, y, 0);
		double k2 = polishFunc.evaluateFunction( x + 0.5*step, y + 0.5*step*k1, 0);
		double k3 = polishFunc.evaluateFunction( x + 0.5*step, y + 0.5*step*k2, 0);
		double k4 = polishFunc.evaluateFunction( x + step, y + step*k3, 0);

		return y + (step/6)*(k1 + 2*k2 + 2*k3 + k4);
	}

	private double getRK4StartAngle(double x, double y, double radius)
	{
		double angle = 0;
		double startAngleTangent;
		double finalX;
		double finalY;
		double error = 10000;
		for(int i=0; error > Constants.ANGLE_ERROR && i<Constants.MAX_ANGLE_LOOPS; i++)
		{
			finalX = x + radius*Math.cos(angle);
			finalY = y + radius*Math.sin(angle);

			double tempStepSize = Constants.STEP_SIZE;

			double nextY = getRK4(finalX, finalY, tempStepSize);
			double nextX = finalX + tempStepSize;

			startAngleTangent = (nextY-finalY)/(nextX-finalX);

			double newAngle = Math.atan(startAngleTangent);

			error = Math.abs(newAngle - angle);

			angle = newAngle;
		}

		return angle;
	}

	private double[] getRK42(double x, double y, double dy, double step)
	{
		double x1 = x;
		double y1 = y;
		double y2 = dy;

		double k11 = y2;
		double k12 = polishFunc.evaluateFunction( x1, y1, y2);

		x1 = x + step/2;
		y1 = y + (step/2)*k11;
		y2 = dy + (step/2)*k12;

		double k21 = y2;
		double k22 = polishFunc.evaluateFunction( x1, y1, y2);

		y1 = y + (step/2)*k21;
		y2 = dy + (step/2)*k22;

		double k31 = y2;
		double k32 = polishFunc.evaluateFunction( x1, y1, y2);

		x1 = x + step;
		y1 = y + (step)*k31;
		y2 = dy + (step)*k32;

		double k41 = y2;
		double k42 = polishFunc.evaluateFunction( x1, y1, y2);


		y1 = y + (step/6)*(k11 + 2*k21 + 2*k31 + k41);
		y2 = dy + (step/6)*(k12 + 2*k22 + 2*k32 + k42);


		return new double[]{y1, y2};
	}

	private boolean playerAlreadyHit(int player, int soldier)
	{
		for(int i=0; i<numPlayersHit; i++)
		{
			if(playersHit[i] == player && soldiersHit[i]==soldier)
				return true;
		}

		return false;
	}

	public void processRange(Obstacle obstacle, Player players[], int numPlayers, int currentTurn, boolean inverted, int mode, double angle)
	{
		playersHit = new int[numPlayers*Constants.MAX_SOLDIERS_PER_PLAYER];
		soldiersHit = new int[numPlayers*Constants.MAX_SOLDIERS_PER_PLAYER];
		soldierHitPosition = new int[numPlayers*Constants.MAX_SOLDIERS_PER_PLAYER];
		numPlayersHit = 0;

		valuesX = new double[Constants.FUNC_MAX_STEPS];
		valuesY = new double[Constants.FUNC_MAX_STEPS];
		valuesDY = new double[Constants.FUNC_MAX_STEPS];

		double stepSize = Constants.STEP_SIZE;

		Soldier currentTurnSoldier = players[currentTurn].getCurrentTurnSoldier();

		valuesX[0] = currentTurnSoldier.getX();
		valuesY[0] = currentTurnSoldier.getY();

		valuesX[0] = (inverted)?(Constants.PLANE_LENGTH - valuesX[0]):valuesX[0];

		if (mode == Constants.SND_ODE)
		{
			valuesX[0] = valuesX[0] + Constants.SOLDIER_RADIUS*Math.cos(angle);
			valuesY[0] = valuesY[0] - Constants.SOLDIER_RADIUS*Math.sin(angle);
		}


		valuesX[0] = (Constants.PLANE_GAME_LENGTH*(valuesX[0] - Constants.PLANE_LENGTH/2))/Constants.PLANE_LENGTH;
		valuesY[0] = (Constants.PLANE_GAME_LENGTH*(-valuesY[0] + Constants.PLANE_HEIGHT/2))/Constants.PLANE_LENGTH;
		valuesDY[0] = Math.tan(angle);

		double gameCoordinateRadius = ((double)(Constants.PLANE_GAME_LENGTH*Constants.SOLDIER_RADIUS))/Constants.PLANE_LENGTH;

		double fireAngle = Double.NaN;

		if (mode == Constants.NORMAL_FUNC)
			fireAngle = getStartAngle(valuesX[0], gameCoordinateRadius);
		if (mode == Constants.FST_ODE)
			fireAngle = getRK4StartAngle(valuesX[0], valuesY[0], gameCoordinateRadius);
		if (mode == Constants.SND_ODE)
			fireAngle = angle;

		if (mode != Constants.SND_ODE && !Double.isNaN(fireAngle) && !Double.isInfinite(fireAngle))
		{
			valuesX[0] = valuesX[0] + gameCoordinateRadius*Math.cos(fireAngle);
			valuesY[0] = valuesY[0] + gameCoordinateRadius*Math.sin(fireAngle);
		}

		offSet = -polishFunc.evaluateFunction(valuesX[0],0,0) + valuesY[0];

		numSteps = Constants.FUNC_MAX_STEPS;
		for (int i = 1; i < Constants.FUNC_MAX_STEPS; i++)
		{
			double tempStepSize = stepSize;

			valuesX[i] = valuesX[i-1];
			valuesY[i] = valuesY[i-1];
			valuesDY[i] = valuesDY[i-1];

			if (mode == Constants.NORMAL_FUNC)
				valuesY[i] = polishFunc.evaluateFunction(valuesX[i],0,0) + offSet;
			if (mode == Constants.FST_ODE)
				valuesY[i] = getRK4(valuesX[i], valuesY[i], tempStepSize);
			if (mode == Constants.SND_ODE)
			{
				double[] vecY = getRK42(valuesX[i], valuesY[i], valuesDY[i], tempStepSize);
				valuesY[i] = vecY[0];
				valuesDY[i] = vecY[1];
			}
			valuesX[i] = valuesX[i] + tempStepSize;

			boolean endFunc = false;

			for(int j = 0; Math.pow(valuesX[i]-valuesX[i-1], 2) + Math.pow(valuesY[i]-valuesY[i-1], 2) > Constants.FUNC_MAX_STEP_DISTANCE_SQUARED; j++)
			{
				if (valuesX[i] - valuesX[i-1] <= Constants.FUNC_MIN_X_STEP_DISTANCE)
				{
					endFunc = true;
					break;
				}
				tempStepSize = tempStepSize/2;

				valuesX[i] = valuesX[i-1];
				valuesY[i] = valuesY[i-1];
				valuesDY[i] = valuesDY[i-1];

				if (mode == Constants.NORMAL_FUNC)
					valuesY[i] = polishFunc.evaluateFunction(valuesX[i],0,0) + offSet;
				if (mode == Constants.FST_ODE)
					valuesY[i] = getRK4(valuesX[i], valuesY[i], tempStepSize);
				if (mode == Constants.SND_ODE)
				{
					double[] vecY = getRK42(valuesX[i], valuesY[i], valuesDY[i], tempStepSize);
					valuesY[i] = vecY[0];
					valuesDY[i] = vecY[1];
				}
				valuesX[i] = valuesX[i-1] + tempStepSize;
			}
			if(endFunc)
			{
				numSteps = i;
				break;
			}

			double x = Constants.PLANE_LENGTH*valuesX[i]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_LENGTH/2;
			double y = -Constants.PLANE_LENGTH*valuesY[i]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_HEIGHT/2;

			x = (inverted)?(Constants.PLANE_LENGTH - x):x;

			for(int j = 0; j < numPlayers; j++)
			{
				Player player = players[j];
				for(int k = 0; k < player.getNumSoldiers(); k++)
				{
					if(j == currentTurn && k == player.getCurrentTurnSoldierIndex())
						continue;
					if (!player.getSoldiers()[k].isAlive())
						continue;

					double distX = players[j].getSoldiers()[k].getX() - x;
					double distY = players[j].getSoldiers()[k].getY() - y;
					double distSquared = (Math.pow(distX, 2)+Math.pow(distY, 2));

					if(distSquared >= Constants.SOLDIER_RADIUS*Constants.SOLDIER_RADIUS)
						continue;
					if(playerAlreadyHit(j,k))
						continue;

					playersHit[numPlayersHit] = j;
					soldiersHit[numPlayersHit] = k;
					soldierHitPosition[numPlayersHit] = i;
					numPlayersHit++;
				}
			}


			if(obstacle.collidePoint((int)x, (int)y) || Double.isNaN(y) || Double.isInfinite(y))
			{
				numSteps = i;
				break;
			}
		}

		lastX = Constants.PLANE_LENGTH*valuesX[numSteps-1]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_LENGTH/2;
		lastY = -Constants.PLANE_LENGTH*valuesY[numSteps-1]/Constants.PLANE_GAME_LENGTH + Constants.PLANE_HEIGHT/2;
	}

	public double getLastX()
	{
		return lastX;
	}

	public double getLastY()
	{
		return lastY;
	}

}
