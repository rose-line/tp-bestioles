// Des mini-bestioles qui ne sont pas bien dangereuses.
// Elles restent sur place, attendant d'être infectées, mais arrivent
// parfois à infecter d'autres bestioles qui passent devant elles.

package fr.pgah.bestioles;

import java.awt.*;

public class Plancton extends Bestiole {
  public Action getAction(BestioleInfo info) {
    return Action.INFECTER;
  }

  public Color getCouleur() {
    return Color.PINK;
  }

  public String toString() {
    return "P";
  }
}