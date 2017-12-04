import java.awt.FlowLayout;

import javax.swing.JFrame;
import javax.swing.JLabel;

public class DiceGui extends JFrame {
	
	public JLabel diceLabel;
	
	public DiceGui() {
		this.setSize(300, 200);
		this.setVisible(true);
		this.setTitle("Liar's Dice");
		
		FlowLayout layout = new FlowLayout(FlowLayout.CENTER);
		layout.setHgap(100);
		layout.setVgap(20);
		this.setLayout(layout);
		
		diceLabel = new JLabel("Liar's Dice");
		this.add(diceLabel);
		
	}

	public void updateDice(int[] dice) {
		String str = "Your dice are: ";
		for (int i=0; i<dice.length; i++) {
			str += dice[i] + " ";
		}
		str += "\n";
		diceLabel.setText(str);
	}
	
	public void lose() {
		diceLabel.setText("YOU LOSE");
	}
	
	public void win() {
		diceLabel.setText("YOU WIN");
	}

}
