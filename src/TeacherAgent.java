import java.util.Hashtable;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


public class TeacherAgent extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private String vagasDisponiveis;
	private Hashtable<String,Integer> classes;
	
	private ClassesOfferGui myGui;
	
	protected void setup() {
		classes = new Hashtable<String,Integer>();

		myGui = new ClassesOfferGui(this);
		myGui.showGui();
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			vagasDisponiveis = (String) args[0];
			System.out.println("Vagas disponiveis "+vagasDisponiveis);
		}
		DFAgentDescription dfd = new DFAgentDescription();
		dfd.setName(this.getAID());
		ServiceDescription sd = new ServiceDescription();
		sd.setType("classes-offer");
		sd.setName("JADE-classes-offer");
		dfd.addServices(sd);	
		try {
			DFService.register(this, dfd);
		}
		catch (FIPAException fe) {
			fe.printStackTrace();
		}

		this.addBehaviour(new OfferRequestsServer());

		this.addBehaviour(new MatriculatesRequestServer());
	}

	private class OfferRequestsServer extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;

		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				// CFP Message received. Process it
				String classesName = msg.getContent();
				ACLMessage reply = msg.createReply();

				Integer level = (Integer) classes.get(classesName);
				if (level != null) {
					
					reply.setPerformative(ACLMessage.PROPOSE);
					reply.setContent(String.valueOf(level.intValue()));
				}
				else {
					
					reply.setPerformative(ACLMessage.REFUSE);
					reply.setContent("sem disponibilidade");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  
	
	private class MatriculatesRequestServer extends CyclicBehaviour {

		private static final long serialVersionUID = 1L;
		int vagasDisponivel = Integer.parseInt(vagasDisponiveis);
		public void action() {
			MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
			ACLMessage msg = myAgent.receive(mt);
			if (msg != null) {
				
				//vagasDisponivel-= 1;
				// ACCEPT_PROPOSAL Message received. Process it
				String title = msg.getContent();
				Integer level = (Integer) classes.get(title);
				ACLMessage reply = msg.createReply();
					if (vagasDisponivel <= 0){
					 level = (Integer) classes.remove(title);
					}
				if (level != null) {
					reply.setPerformative(ACLMessage.INFORM);
					System.out.println(msg.getSender().getName()+ "matriculado em : "+title);
					vagasDisponivel--;
				}
				else {
					reply.setPerformative(ACLMessage.FAILURE);
					reply.setContent("não disponivel");
				}
				myAgent.send(reply);
			}
			else {
				block();
			}
		}
	}  // End of inner class OfferRequestsServer
	
	/**
    This is invoked by the GUI when the user adds a new book for sale
	 */
	public void updateClasses(final String title, final int level) {
		addBehaviour(new OneShotBehaviour() {

			private static final long serialVersionUID = 1L;

			public void action() {
				classes.put(title, new Integer(level));
				System.out.println(title+" inserido no sistem com o nivel = "+level);
			}
		} );
	}
	
	
	// Put agent clean-up operations here
		protected void takeDown() {
			// Deregister from the yellow pages
			try {
				DFService.deregister(this);
			}
			catch (FIPAException fe) {
				fe.printStackTrace();
			}
			// Close the GUI
			myGui.dispose();
			// Printout a dismissal message
			System.out.println("Teacher-agent "+getAID().getName()+" terminating.");
		}

}
