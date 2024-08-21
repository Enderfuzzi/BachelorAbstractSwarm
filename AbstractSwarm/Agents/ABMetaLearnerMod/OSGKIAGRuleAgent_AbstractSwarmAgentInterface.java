


import java.util.HashMap;
import java.util.List;
import java.util.Random;


/**
 * This class provides the three methods of the AbstractSwarm agent interface 
 * for state perception and performing actions, communication with other agents
 * and the perception of rewards.<br />
 * <br />
 * 
 * The properties of the interface's objects are:<br />
 * <br />
 * 
 * AGENT:<br />
 * .name           the agent's name<br />
 * .type           the agent's AGENT_TYPE (see below)<br />
 * .frequency      the number of remaining visits; -1 if the agent's type has
 *                 no frequency attribute<br />
 * .necessities    the number of remaining visits for each connected station,
 *                 -1 if the agent's type has no necessity attribute<br />          
 * .cycles         the number of remaining cycles for each incoming visit edge;
 *                 -1 if agent's type has no cycle attribute<br />
 * .time           the remaining time on the target station, -1 if agent is
 *                 currently not visiting a station or if agent's type has no
 *                 time attribute<br />
 * .target         the agent's current target<br />
 * .visiting       whether the agent is currently visiting a station<br />
 * <br />
 * 
 * STATION:<br />
 * .name           the station's name<br />
 * .type           the station's STATION_TYPE (see below)<br />
 * .frequency      the number of remaining visits; -1 if the station's type
 *                 has no frequency attribute<br />
 * .necessities    the number of remaining visits for each connected agent,
 *                 -1 if the station's type has no necessity attribute<br />          
 * .cycles         the number of remaining cycles for each incoming visit edge;
 *                 -1 if station's type has no cycle attribute<br />
 * .space          the remaining space, -1 if the station's type has no space
 *                 attribute<br />
 * <br />
 * 
 * AGENT_TYPE:<br />
 * .name           the agent type's name as string<br />
 * .type           the type as string ("AGENT_TYPE")<br />
 * .components     the agent type's AGENTs (see above)<br />
 * .frequency      the agent type's frequency attribute; -1 if the agent type 
 *                 has no frequency attribute<br />
 * .necessity      the agent type's necessity attribute; -1 if the agent type 
 *                 has no necessity attribute<br />          
 * .cycle          the agent type's cycle attribute; -1 if agent type has no
 *                 cycle attribute<br />
 * .time           the agent type's time attribute, -1 if agent type has no
 *                 time attribute<br />
 * .size           the agent type's size attribute, -1 if agent type has no
 *                 size attribute<br />
 * .priority       the agent type's priority attribute, -1 if agent type has
 *                 no priority attribute<br />
 * .visitEdges     the agent type's VISIT_EDGEs as list (see below)<br />
 * .timeEdges      the agent type's TIME_EDGEs as list (see below)<br />
 * .placeEdges     the agent type's PLACE_EDGEs as list (see below)
 *                 (note that, by definition, agent types cannot have place
 *                 edges and thus this list will always be empty; this is only
 *                 for backwards compatibility/unification reasons)<br />
 * <br />
 *
 * STATION_TYPE:<br />
 * .name           the station type's name as string<br />
 * .type           the type as string ("STATION_TYPE")<br />
 * .components     the station type's stations<br />
 * .frequency      the station type's frequency attribute; -1 if the station
 *                 type has no frequency attribute<br />
 * .necessity      the station type's necessity attribute; -1 if the station
 *                 type has no necessity attribute<br />          
 * .cycle          the station type's cycle attribute; -1 if station type has
 *                 no cycle attribute<br />
 * .time           the station type's time attribute, -1 if station type has no
 *                 time attribute<br />
 * .space          the station type's space attribute, -1 if station type has
 *                 no space attribute<br />
 * .visitEdges     the station type's VISIT_EDGEs as list (see below)<br />
 * .timeEdges      the station type's TIME_EDGEs as list (see below)<br />
 * .placeEdges     the station type's PLACE_EDGEs as list (see below)<br />
 * <br />
 *
 * VISIT EDGE:<br />
 * .type           the edge's type as string ("VISIT_EDGE")<br />
 * .connectedType  the opposite component type connected by the edge<br />
 * .bold           whether the edge is bold<br />
 * <br />
 * 
 * TIME EDGE:<br />
 * .type           the edge's type as string ("TIME_EDGE")<br />
 * .connectedType  the opposite component type connected by the edge<br />
 * .incoming       whether the edge is incoming<br />
 * .outgoing       whether the edge is outgoing<br />
 * .andConnected   whether the edge is and-connected to the opposite type<br />
 * .andOrigin      whether the edge is and-connected at its origin type<br />
 * <br />
 * 
 * PLACE EDGE:<br />
 * .type         the edge's type as string ("PLACE_EDGE")<br />
 * .connectedType  the opposite component type connected by the edge<br />
 * .incoming       whether the edge is incoming<br />
 * .outgoing       whether the edge is outgoing<br />
 */
public class OSGKIAGRuleAgent_AbstractSwarmAgentInterface 
{
	/** The random generator used for random decisions. */
	private static Random random = new Random();
	
	
	/**
	 * This method allows an agent to perceive its current state and to perform
	 * actions by returning an evaluation value for potential next target
	 * stations. The method is called for every station that can be visted by
	 * the agent. 
	 * 
	 * @param me        the agent itself
	 * @param others    all other agents in the scenario with their currently
	 *                  communicated information
	 * @param stations  all stations in the scenario
	 * @param time      the current time unit
	 * @param station   the station to be evaluated as potential next target
	 * 
	 * @return          the evaluation value of the station
	 */
	public static double evaluation( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Station station )
	{
		
		// Bewertung mit 0 initialisieren
		double bewertung = 0;
		
		
		
		
		// Wenn ich Patient1 bin und nach  und Xray1 gefragt werde, dann hoch bewerten
		if(me.name.equals( "Patient.1" ) && station.name.equals( "XRay1.1") )
		{
			bewertung = 1;
		}
		
		// Wenn ich Patient2 bin und nach  und Xray2 gefragt werde, dann hoch bewerten
		if(me.name.equals( "Patient.2" ) && station.name.equals( "XRay2.1") )
		{
			bewertung = 1;
		}
		
		// Wenn ich Patient3 bin und nach  und Xray3 gefragt werde, dann hoch bewerten
		if(me.name.equals( "Patient.3" ) && station.name.equals( "XRay3.1") )
		{
			bewertung = 1;
		}
		
		// Wenn ich Patient4 bin und nach  und Xray1 gefragt werde, dann hoch bewerten
		if(me.name.equals( "Patient.4" ) && station.name.equals( "XRay1.1") )
		{
			bewertung = 1;
		}
		
		// Wenn ich Patient5 bin und nach Xray2 gefragt werde, dann hoch bewerten
		if(me.name.equals( "Patient.5" ) && station.name.equals( "XRay2.1") )
		{
			bewertung = 1;
		}
		
		//Wenn ich Produkt1 bin und nach Shelf1 gefragt werde, dann hoch bewerten
		if(me.name.equals( "Product.1" ) && station.name.equals( "Shelf1.1")  )
		{
		    bewertung = 1;
		}
		
		//Wenn ich Produkt2 bin und nach Shelf1 gefragt werde, dann hoch bewerten
		if(me.name.equals( "Product.2" ) && station.name.equals( "Shelf1.1")  )
		{
		    bewertung = 1;
		}
		
		//Wenn ich Produkt3 bin und nach Shelf1 gefragt werde, dann hoch bewerten
		if(me.name.equals( "Product.3" ) && station.name.equals( "Shelf1.1")  )
		{
		    bewertung = 1;
		}
		
		//Wenn ich Produkt4 bin und nach Shelf1 gefragt werde, dann hoch bewerten
		if(me.name.equals( "Product.4" ) && station.name.equals( "Shelf1.1")  )
		{
		    bewertung = 1;
		}
		
		//Wenn ich Produkt5 bin und nach Shelf1 gefragt werde, dann hoch bewerten
		if(me.name.equals( "Product.5" ) && station.name.equals( "Shelf1.1")  )
		{
		    bewertung = 1;
		}
		
		//Wenn ich Produkt6 bin und nach Shelf1 gefragt werde, dann hoch bewerten
		if(me.name.equals( "Product.6" ) && station.name.equals( "Shelf1.1")  )
		{
		    bewertung = 1;
		}
		
		//Wenn ich Produkt7 bin und nach Shelf1 gefragt werde, dann hoch bewerten
		if(me.name.equals( "Product.7" ) && station.name.equals( "Shelf1.1")  )
		{
		    bewertung = 1;
		}
		
		//Wenn ich Produkt8 bin und nach Shelf1 gefragt werde, dann hoch bewerten
		if(me.name.equals( "Product.8" ) && station.name.equals( "Shelf1.1")  )
		{
		    bewertung = 1;
		}
		
		//Wenn ich Produkt9 bin und nach Shelf1 gefragt werde, dann hoch bewerten
		if(me.name.equals( "Product.9" ) && station.name.equals( "Shelf1.1")  )
		{
		    bewertung = 1;
		}
		
		//Wenn ich Produkt10 bin und nach Shelf1 gefragt werde, dann hoch bewerten
	    if(me.name.equals( "Product.10" ) && station.name.equals( "Shelf1.1")  )
		{
		    bewertung = 1;
		}
		
		//Wenn ich Deliverer1 bin und nach Costumer1 gefragt werde dann am hoehsten bewerten 
	    if(me.name.equals( "Deliverer.1" ) && station.name.equals( "Customer1.1") )
	    {
	    	bewertung = 5;
	    }
	    
	    //Wenn ich Deliverer1 bin und nach Costumer4 gefragt werde dann am hoch bewerten 
	    if(me.name.equals( "Deliverer.1" ) && station.name.equals( "Customer2.1") )
	    {
	    	bewertung = 3;
	    }
	    
	    //Wenn ich Deliverer2 bin und nach Costumer1 gefragt werde dann am hoehsten bewerten 
	    if(me.name.equals( "Deliverer.2" ) && station.name.equals( "Customer3.1") )
	    {
	    	bewertung = 5;
	    }
	    
	    //Wenn ich Deliverer2 bin und nach Costumer1 gefragt werde dann hoch bewerten 
	    if(me.name.equals( "Deliverer.2" ) && station.name.equals( "Customer4.1") )
	    {
	    	bewertung = 3;
	    }
	    
	  //Wenn ich Deliverer3 bin und nach Costumer3 gefragt werde dann am hoehsten bewerten 
	    if(me.name.equals( "Deliverer.3" ) && station.name.equals( "Customer5.1") )
	    {
	    	bewertung = 5;
	    }
	    
	  //Wenn ich Deliverer3 bin und nach Costumer5 gefragt werde dann am hoehsten bewerten 
	    if(me.name.equals( "Deliverer.3" ) && station.name.equals( "Customer6.1") )
	    {
	    	bewertung = 3;
	    }

		
		
		
		// Wenn ich Patient1 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
		if( me.name.equals( "Patient.1") && station.name.equals( "Treatment1.1" ))
		{
				bewertung = 10;
		}
		
		// Wenn ich Patient1 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
		if( me.name.equals( "Patient.1") && station.name.equals( "Treatment2.1" ))
		{
				bewertung = 20;
		}
		// Wenn ich Patient1 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
		if( me.name.equals( "Patient.1") && station.name.equals( "Treatment3.1" ))
		{
				bewertung = 30;
		}

		// Wenn ich Patient2 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
		if( me.name.equals( "Patient.2") && station.name.equals( "Treatment1.1" ))
		{
				bewertung = 20;
		}
		
		// Wenn ich Patient2 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
		if( me.name.equals( "Patient.2") && station.name.equals( "Treatment2.1" ))
		{
				bewertung = 30;
		}
		
		// Wenn ich Patient2 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
		if( me.name.equals( "Patient.2") && station.name.equals( "Treatment3.1" ))
		{
				bewertung = 10;
		}

		// Wenn ich Patient3 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
		if( me.name.equals( "Patient.3") && station.name.equals( "Treatment1.1" ))
		{
				bewertung = 30;
		}
		
		// Wenn ich Patient3 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
				if( me.name.equals( "Patient.3") && station.name.equals( "Treatment2.1" ))
		{
				bewertung = 10;
		}
				
		// Wenn ich Patient3 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
				if( me.name.equals( "Patient.3") && station.name.equals( "Treatment3.1" ))
		{
				bewertung = 20;
		}
		
		// Wenn ich Patient4 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
				if( me.name.equals( "Patient.4") && station.name.equals( "Treatment1.1" ))
		{
				bewertung = 10;
		}
		
		// Wenn ich Patient4 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
				if( me.name.equals( "Patient.4") && station.name.equals( "Treatment2.1" ))
		{
				bewertung = 20;
		}
				
		// Wenn ich Patient4 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
				if( me.name.equals( "Patient.4") && station.name.equals( "Treatment3.1" ))
		{
				bewertung = 30;
		}
				
		
				
		// Wenn ich Patient5 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
				if( me.name.equals( "Patient.5") && station.name.equals( "Treatment2.1" ))
		{
				bewertung = 20;
		}
				
		// Wenn ich Patient5 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
				if( me.name.equals( "Patient.5") && station.name.equals( "Treatment3.1" ))
		{
				bewertung = 30;
		}
						
		// Wenn ich Patient5 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
				if( me.name.equals( "Patient.5") && station.name.equals( "Treatment1.1" ))
		{
				bewertung = 10;
		}
				
		
				// Wenn ich Patient6 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
				if( me.name.equals( "Patient.6") && station.name.equals( "Treatment1.1" ))
				{
						bewertung = 30;
				}
				
				// Wenn ich Patient6 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
				if( me.name.equals( "Patient.6") && station.name.equals( "Treatment2.1" ))
				{
						bewertung = 10;
				}
				// Wenn ich Patient6 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
				if( me.name.equals( "Patient.6") && station.name.equals( "Treatment3.1" ))
				{
						bewertung = 20;
				}

				// Wenn ich Patient7 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
				if( me.name.equals( "Patient.7") && station.name.equals( "Treatment1.1" ))
				{
						bewertung = 10;
				}
				
				// Wenn ich Patient7 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
				if( me.name.equals( "Patient.7") && station.name.equals( "Treatment2.1" ))
				{
						bewertung = 20;
				}
				
				// Wenn ich Patient7 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
				if( me.name.equals( "Patient.7") && station.name.equals( "Treatment3.1" ))
				{
						bewertung = 30;
				}

				// Wenn ich Patient8 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
				if( me.name.equals( "Patient.8") && station.name.equals( "Treatment1.1" ))
				{
						bewertung = 20;
				}
				
				// Wenn ich Patient8 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
						if( me.name.equals( "Patient.8") && station.name.equals( "Treatment2.1" ))
				{
						bewertung = 30;
				}
						
				// Wenn ich Patient8 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
						if( me.name.equals( "Patient.8") && station.name.equals( "Treatment3.1" ))
				{
						bewertung = 10;
				}
				
				// Wenn ich Patient9 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
						if( me.name.equals( "Patient.9") && station.name.equals( "Treatment1.1" ))
				{
						bewertung = 30;
				}
				
				// Wenn ich Patient9 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
						if( me.name.equals( "Patient.9") && station.name.equals( "Treatment2.1" ))
				{
						bewertung = 10;
				}
						
				// Wenn ich Patient9 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
						if( me.name.equals( "Patient.9") && station.name.equals( "Treatment3.1" ))
				{
						bewertung = 20;
				}
						
				// Wenn ich Patient10 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
						if( me.name.equals( "Patient.10") && station.name.equals( "Treatment1.1" ))
				{
						bewertung = 10;
				}
						
				// Wenn ich Patient10 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
						if( me.name.equals( "Patient.10") && station.name.equals( "Treatment2.1" ))
				{
						bewertung = 20;
				}
						
				// Wenn ich Patient10 bin und nach Station XRay2 gefragt werde, dann hoch bewerten
						if( me.name.equals( "Patient.10") && station.name.equals( "Treatment3.1" ))
				{
						bewertung = 30;
				}
								
						
		
		
				
		//Kontinuieren mit Agent 7,8,9 und 10 dann auf die Zeit Achten
																
		
				
				
				
				
				
				
				
				
				
				
				
				
				//Name des aktuellen Agenten ausgeben
		System.out.println( time + "   " + me.name + "   " + station.name + "   " + bewertung ) ;
		
		return bewertung;
	}
	
	
	/**
	 * This method allows an agent to communicate with other agents by
	 * returning a communication data object.
	 * 
	 * @param me           the agent itself
	 * @param others       all other agents in the scenario with their
	 *                     currently communicated information
	 * @param stations     all stations in the scenario
	 * @param time         the current time unit
	 * @param defaultData  a triple (selected station, time unit when the 
	 *                     station is reached, evaluation value of the station)
	 *                     that can be used for default communication
	 * 
	 * @return             the communication data object
	 */
	public static Object communication( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, Object[] defaultData )
	{
		return null;
	}

	
	/**
	 * This method allows an agent to perceive the local reward for its most
	 * recent action.
	 * 
	 * @param me           the agent itself
	 * @param others       all other agents in the scenario with their
	 *                     currently communicated information
	 * @param stations     all stations in the scenario
	 * @param time         the current time unit
	 * @param value        the local reward in [0, 1] for the agent's most
	 *                     recent action 
	 */
	public static void reward( Agent me, HashMap<Agent, Object> others, List<Station> stations, long time, double value )
	{
	}
}
