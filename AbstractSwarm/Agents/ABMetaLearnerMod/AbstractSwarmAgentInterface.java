/*
AbstractSwarm agent that makes random decisions
Copyright (C) 2020  Daan Apeldoorn (daan.apeldoorn@uni-mainz.de)

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/


import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
public class AbstractSwarmAgentInterface 
{
	/** The random generator used for random decisions. */
	private static Random random = new Random();
	
	/** Letzte Zeit der Evaluation */
	private static long letzteZeit = -1;
	
	/** Die momentan gewaehlte Agentenimplementierung initialisieren */
	private static int momentanerAgent = -1;
	
	/** Speichert die letzte Aktion der Agenten */
	private static Map<Agent, Station> letzteAktionen = new HashMap<Agent, Station>();
	
	/** Speichert die Zeitpunkte der letzten Aktion der Agenten */
	private static Map<Agent, Long> aktionsZeitpunkte = new HashMap<Agent, Long>();

	/** "Gehirne" der Agenten zum Speichern der Belohnungen */
	private static Map<Agent, Map<String, Double>> gehirne = new HashMap<Agent, Map<String, Double>>();
	
	/** Zaehler fuer Durchlaeufe initialisieren */
	private static int anzahlLaeufe = 0;
	
	
/** Lernrate    ----->   Vermutlich noch anpassen!!! */
	private static double lernrate = 0.2;
	
/** (Ende der) Explorationsspanne    ----->   Vermutlich noch anpassen!!! */
	private static double explorationsspanne = 30;
	
/** (Anfang der) Exploitationsspanne    ----->   Vermutlich noch anpassen!!! */
	private static double exploitationsspanne = 80;
	
	/** Explorationsrate initialisieren (wird spaeter automatisch angepasst) */
	private static double explorationsrate = 1;
	
	/** Subrahend zum Abklingen der Explorationsrate (Explorationsrate soll im Laufe der Laeufe immer von 1 auf 0 gesenkt werden) */
	private static double abklingungssubtrahend = 1 / (exploitationsspanne - explorationsspanne);
	
	
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
		// Zeitpunkt der letzten aktion merken
		aktionsZeitpunkte.put( me, time );
		
		// Anzahl Laeufe zaehlen	FRAGE: ist die zweite Bedingung nicht IMMER erfuellt?
		if( time == 1 && time != letzteZeit)
		{
			anzahlLaeufe = anzahlLaeufe + 1;
		
			// Aktuellen Lauf ausgeben
			System.out.print( anzahlLaeufe );
			System.out.println( ". Simulationslauf hat begonnen!" );
			
			// Zufaellige Auswahl einer Agentenimplementierung   =>   Parameter in Klammern = Anzahl Agentenimplementierungen
			momentanerAgent = random.nextInt( 6 );
			
			
			// Explorationsrate je nach aktuellem Lauf anpassen
			if( anzahlLaeufe <= explorationsspanne ) {
				explorationsrate = 1;	// nur explorieren
			}
			else if( anzahlLaeufe >= explorationsspanne && anzahlLaeufe <= exploitationsspanne ) {
				explorationsrate = explorationsrate - abklingungssubtrahend;	// Explorationsrate abklingen lassen
			}
			else { // if(anzahlLaeufe >= exploitationsspanne)
				explorationsrate = 0;	// nur exploitieren
			}
		}
		
		letzteZeit = time;
		
		// Bewertung zufaellig initialisieren
		double bewertung = random.nextDouble();
		
		
		// Aktionswahl weitergeben an gewaehlte Agentenimplementierung
		if( momentanerAgent == 0 ) {
				bewertung = AgentLearning_AbstractSwarmAgentInterface.evaluation(me, others, stations, time, station);
		}
		else if( momentanerAgent == 1 ) {
				bewertung = AgentPlace_AbstractSwarmAgentInterface.evaluation(me, others, stations, time, station);
		}
		else if( momentanerAgent == 2 ) {
				bewertung = AgentProb_AbstractSwarmAgentInterface.evaluation(me, others, stations, time, station);
		}
		else if( momentanerAgent == 3 ) {
				bewertung = AgentPWQPlusQueue_AbstractSwarmAgentInterface.evaluation(me, others, stations, time, station);
		}
		else if( momentanerAgent == 4 ) {
				bewertung = AgentPWQPlusSOM_AbstractSwarmAgentInterface.evaluation(me, others, stations, time, station);
		}
		else if( momentanerAgent == 5 ) {
				bewertung = AgentReactiveMaxFreeSpace_AbstractSwarmAgentInterface.evaluation(me, others, stations, time, station);
		}
//		else if( momentanerAgent == 6 ) {
//				bewertung = AgentReactiveMinDistance_AbstractSwarmAgentInterface.evaluation(me, others, stations, time, station);
//		}
//		else if( momentanerAgent == 7 ) {
//				bewertung = OSGKIAGLearningAgent_AbstractSwarmAgentInterface.evaluation(me, others, stations, time, station);
//		}
//		else if( momentanerAgent == 8 ) {
//				bewertung = OSGKIAGRuleAgent_AbstractSwarmAgentInterface.evaluation(me, others, stations, time, station);
//		}
		
		
		// Exploitationsrate nutzen, um zu entscheiden, ob exploitiert werden soll:
		if( explorationsrate != 1 && random.nextFloat() <= (1 - explorationsrate) ) {
			
			// Kombination aus Zeitpunkt und Aktion bilden
			String zeitpunktAktion = time + "_" + station.name;
			
			// Gerlernten Bewertungswert abrufen
			if( gehirne.containsKey ( me ) && gehirne.get ( me ).containsKey( zeitpunktAktion ) ) {
				bewertung = gehirne.get( me ).get( zeitpunktAktion );
			}
			// Wenn nichts gelernt wurde, explorieren (bewertung nicht ueberschreiben und Aktionswahl zufaellig lassen)
//			else {
//				// Was ist an dieser Stelle die sinnvollste Bewertung?
//				// bewertung = 0;
//				// bewertung = AgentReactiveMinDistance_AbstractSwarmAgentInterface.evaluation(me, others, stations, time, station);
//			}
		}

		// Name des aktuellen Agenten und Station mit Bewertung ausgeben
		System.out.println( momentanerAgent + "," + time + "   " + me.name + "   " + station.name + "   " + bewertung);

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
		//Merken was die letzte Aktion war
		letzteAktionen.put( me,  (Station) defaultData [0] );
		
		
		// Kommunikation weitergeben an gewaehlte Agentenimplementierung
		if( momentanerAgent == 0 ) {
				return AgentLearning_AbstractSwarmAgentInterface.communication(me, others, stations, time, defaultData);
		}
		else if( momentanerAgent == 1 ) {
				return AgentPlace_AbstractSwarmAgentInterface.communication(me, others, stations, time, defaultData);
		}
		else if( momentanerAgent == 2 ) {
				return AgentProb_AbstractSwarmAgentInterface.communication(me, others, stations, time, defaultData);
		}
		else if( momentanerAgent == 3 ) {
				return AgentPWQPlusQueue_AbstractSwarmAgentInterface.communication(me, others, stations, time, defaultData);
		}
		else if( momentanerAgent == 4 ) {
				return AgentPWQPlusSOM_AbstractSwarmAgentInterface.communication(me, others, stations, time, defaultData);
		}
		else if( momentanerAgent == 5 ) {
				return AgentReactiveMaxFreeSpace_AbstractSwarmAgentInterface.communication(me, others, stations, time, defaultData);
		}
		else if( momentanerAgent == 6 ) {
				return AgentReactiveMinDistance_AbstractSwarmAgentInterface.communication(me, others, stations, time, defaultData);
		}
//		else if( momentanerAgent == 7 ) {
//				return OSGKIAGLearningAgent_AbstractSwarmAgentInterface.communication(me, others, stations, time, defaultData);
//		}
//		else if( momentanerAgent == 8 ) {
//				return OSGKIAGRuleAgent_AbstractSwarmAgentInterface.communication(me, others, stations, time, defaultData);
//		}
		
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
		// Reward weitergeben an gewaehlte Agentenimplementierung
		if( momentanerAgent == 0 ) {
				AgentLearning_AbstractSwarmAgentInterface.reward(me, others, stations, time, value);
		}
		else if( momentanerAgent == 1 ) {
				AgentPlace_AbstractSwarmAgentInterface.reward(me, others, stations, time, value);
		}
		else if( momentanerAgent == 2 ) {
				AgentProb_AbstractSwarmAgentInterface.reward(me, others, stations, time, value);
		}
		else if( momentanerAgent == 3 ) {
				AgentPWQPlusQueue_AbstractSwarmAgentInterface.reward(me, others, stations, time, value);
		}
		else if( momentanerAgent == 4 ) {
				AgentPWQPlusSOM_AbstractSwarmAgentInterface.reward(me, others, stations, time, value);
		}
		else if( momentanerAgent == 5 ) {
				AgentReactiveMaxFreeSpace_AbstractSwarmAgentInterface.reward(me, others, stations, time, value);
		}
		else if( momentanerAgent == 6 ) {
				AgentReactiveMinDistance_AbstractSwarmAgentInterface.reward(me, others, stations, time, value);
		}
//		else if( momentanerAgent == 7 ) {
//				OSGKIAGLearningAgent_AbstractSwarmAgentInterface.reward(me, others, stations, time, value);
//		}
//		else if( momentanerAgent == 8 ) {
//				OSGKIAGRuleAgent_AbstractSwarmAgentInterface.reward(me, others, stations, time, value);
//		}
		
		
		
		// Gehirn fuer Agenten anlegen wenn nicht vorhanden
		if(!gehirne.containsKey(me))
		{
			gehirne.put(me, new HashMap<String, Double>() );
		}
		
		// Kombination aus Zeitpunkt und Aktion bilden
		String zeitpunktAktion = aktionsZeitpunkte.get(me) + "_" + letzteAktionen.get(me).name;
		
		// Wenn fuer diese Kombination noch nichts gelernt wurde
		if( !gehirne.get(me).containsKey(zeitpunktAktion))
		{
			// Belohnung einfach merken
			gehirne.get(me).put(zeitpunktAktion, lernrate * value);
		}
		// Sonst alte Belohnung mit neue Belohnung und Lernrate verrechnen
		else
		{
			gehirne.get(me).put(zeitpunktAktion, gehirne.get(me).get(zeitpunktAktion) * (1 - lernrate) + value * lernrate);
		}
	}
}
