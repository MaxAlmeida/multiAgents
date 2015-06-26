import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;


public class StudentAgent  extends Agent {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private String intendedClassesTitle;
	private String studentProfile;
	// The list of known seller agents
	private AID[] teacherAgents;

	// Put agent initializations here
	protected void setup() {
		// Printout a welcome message
		System.out.println("Hello! Student-agent "+getAID().getName()+" is ready.");

		// Get the title of the book to buy as a start-up argument
		Object[] args = getArguments();
		if (args != null && args.length > 0) {
			intendedClassesTitle = (String) args[0];
			studentProfile = (String) args[1];
			System.out.println("Intended classes "+intendedClassesTitle);

			// Add a TickerBehaviour that schedules a request to seller agents every 10 seconds
			addBehaviour(new TickerBehaviour(this, 10000) {

				private static final long serialVersionUID = 1L;

				protected void onTick() {
					System.out.println("Tentando matricular:  "+intendedClassesTitle);
					// Update the list of seller agents
					DFAgentDescription template = new DFAgentDescription();
					ServiceDescription sd = new ServiceDescription();
					sd.setType("classes-offer");
					template.addServices(sd);
					try {
						DFAgentDescription[] result = DFService.search(myAgent, template); 
						System.out.println("Encontrado os seguintes professores:");
						teacherAgents = new AID[result.length];
						for (int i = 0; i < result.length; ++i) {
							teacherAgents[i] = result[i].getName();
							System.out.println(teacherAgents[i].getName());
						}
					}
					catch (FIPAException fe) {
						fe.printStackTrace();
					}

					// Perform the request
					myAgent.addBehaviour(new RequestPerformer());
				}
			} );
		}
		else {
			// Make the agent terminate
			System.out.println("Nenhuma matéria do tipo foi encontrada");
			doDelete();
		}
	}
	
	/**
	   Inner class RequestPerformer.
	   This is the behaviour used by Book-buyer agents to request seller 
	   agents the target book.
	 */
	private class RequestPerformer extends Behaviour {

		private static final long serialVersionUID = 1L;
		private AID bestSeller; // The agent who provides the best offer 
		private int bestPrice;  // The best offered price
		private int repliesCnt = 0; // The counter of replies from seller agents
		private MessageTemplate mt; // The template to receive replies
		private int step = 0;

		public void action() {
			switch (step) {
			case 0:
				// Send the cfp to all sellers
				ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
				for (int i = 0; i < teacherAgents.length; ++i) {
					cfp.addReceiver(teacherAgents[i]);
				} 
				cfp.setContent(intendedClassesTitle);
				cfp.setConversationId("classes-offer");
				cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
				myAgent.send(cfp);
				// Prepare the template to get proposals
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("classes-offer"),
						MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
				step = 1;
				break;
			case 1:
				// Receive all proposals/refusals from seller agents
				ACLMessage reply = myAgent.receive(mt);
				if (reply != null) {
					// Reply received
					if (reply.getPerformative() == ACLMessage.PROPOSE) {
						// This is an offer 
						int price = Integer.parseInt(reply.getContent());
						
						if (bestSeller == null || price < bestPrice  ) {
							// This is the best offer at present
							bestPrice = price;
							bestSeller = reply.getSender();
						}
						if ( Integer.parseInt(studentProfile) < bestPrice)
						{
							System.out.println("Nivel de comprometimento muito abaixo do requerido");
							break;
						}
					}
					repliesCnt++;
					if (repliesCnt >= teacherAgents.length) {
						// We received all replies
						step = 2; 
					}
				}
				else {
					block();
				}
				break;
			case 2:
				// Send the purchase order to the seller that provided the best offer
				ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
				order.addReceiver(bestSeller);
				order.setContent(intendedClassesTitle);
				order.setConversationId("classes-offer");
				order.setReplyWith("order"+System.currentTimeMillis());
				myAgent.send(order);
				// Prepare the template to get the purchase order reply
				mt = MessageTemplate.and(MessageTemplate.MatchConversationId("classes-offer"),
						MessageTemplate.MatchInReplyTo(order.getReplyWith()));
				step = 3;
				break;
			case 3:      
				// Receive the purchase order reply
				reply = myAgent.receive(mt);
				if (reply != null) {
					// Purchase order reply received
					if (reply.getPerformative() == ACLMessage.INFORM) {
						// Purchase successful. We can terminate
						System.out.println(intendedClassesTitle+" Matriculado com sucesso com o professor: "+reply.getSender().getName());
						System.out.println("Price = "+bestPrice);
						myAgent.doDelete();
					}
					else {
						System.out.println("Não há vagas mais disponíveis nessa disciplina.");
					}

					step = 4;
				}
				else {
					block();
				}
				break;
			}        
		}

		public boolean done() {
			
			if (step == 2 && bestSeller == null) {
				System.out.println("Matricula na disciplina "+intendedClassesTitle+" falhou. Não há discplinas disponiveis");
			}
			
			boolean bookIsNotAvailable = (step == 2 && bestSeller == null);
			boolean negotiationIsConcluded = (step == 4);
			
			boolean isDone = false;
			if (bookIsNotAvailable || negotiationIsConcluded) {
				isDone = true;
			}
			else {
				isDone = false;
			}
			
			return isDone;
			//return ((step == 2 && bestSeller == null) || step == 4);
		}
	}  // End of inner class RequestPerformer
	
	
	// Put agent clean-up operations here
	protected void takeDown() {
		// Printout a dismissal message
		System.out.println("Estudante agente "+getAID().getName()+" Finalizando.");
	}
	
}
	

