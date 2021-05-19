// C'est dans le modèle qu'on conserve l'état courant de la simulation.

package fr.pgah.bestioles;

import java.util.*;
import java.awt.Point;
import java.awt.Color;
import java.lang.reflect.*;

public class BestioleModel {
  // Cette constante indique à quelle fréquence INFECTER devrait échouer
  // pour les bestioles qui n'ont pas sauté au mouvement précédent.
  // 0.0 : aucun avantage
  // 1.0 : 100 % avantagé
  public static final double AVANTAGE_SI_SAUT = 0.2; // 20 % d'avantage

  private int hauteur;
  private int largeur;
  private Bestiole[][] grille;
  private Map<Bestiole, PrivateData> infosBestioles;
  private SortedMap<String, Integer> comptageBestioles;
  private boolean debugView;
  private int simulationSteps;

  public BestioleModel(int largeur, int hauteur) {
    this.largeur = largeur;
    this.hauteur = hauteur;
    grille = new Bestiole[largeur][hauteur];
    infosBestioles = new HashMap<Bestiole, PrivateData>();
    comptageBestioles = new TreeMap<String, Integer>();
    this.debugView = false;
  }

  public Iterator<Bestiole> iterator() {
    return infosBestioles.keySet().iterator();
 }

  public Point getPoint(Bestiole c) {
    return infosBestioles.get(c).coord;
  }

  public Color getColor(Bestiole c) {
    return infosBestioles.get(c).color;
  }

  public String getString(Bestiole c) {
    return infosBestioles.get(c).string;
  }

  public void ajouterBestioles(int nb, Class<? extends Bestiole> bestiole) {
    Bestiole.Direction[] directions = Bestiole.Direction.values();

    if (infosBestioles.size() + nb > largeur * hauteur) {
      throw new RuntimeException("Trop de bestioles !");
    }

    for (int i = 0; i < nb; i++) {
      InitialiserBestiole(bestiole, directions);
    }

    String name = bestiole.getName();
    if (!comptageBestioles.containsKey(name))
      comptageBestioles.put(name, nb);
    else
      comptageBestioles.put(name, comptageBestioles.get(name) + nb);
  }

  private void InitialiserBestiole(Class<? extends Bestiole> bestiole, Bestiole.Direction[] directions) {
    Bestiole nouvelle = null;
    try {
      nouvelle = instancierBestiole(bestiole);
    } catch (IllegalArgumentException e) {
      System.out.println("ERREUR : " + bestiole + " n'a pas de constructeur approprié.");
      System.exit(1);
    } catch (Exception e) {
      System.out.println("ERREUR : " + bestiole + " a lancé une exception depuis son constructeur.");
      System.exit(1);
    }

    // Positionnement de la bestiole sur une case libre
    Random r = new Random();
    int x, y;
    do {
      x = r.nextInt(largeur);
      y = r.nextInt(hauteur);
    } while (grille[x][y] != null);
    grille[x][y] = nouvelle;

    Bestiole.Direction dir = directions[r.nextInt(directions.length)];
    infosBestioles.put(nouvelle, new PrivateData(new Point(x, y), dir));
  }

  private Bestiole instancierBestiole(Class bestiole) throws Exception {
    Constructor c = bestiole.getConstructors()[0];
    String debug = bestiole.toString();
    if (bestiole.toString().equals("class fr.pgah.bestioles.Ours")) {
      // pile ou face pour les ours
      boolean polaire = Math.random() < 0.5;
      return (Bestiole) c.newInstance(new Object[] { polaire });
    } else {
      return (Bestiole) c.newInstance();
    }
  }

  public int getWidth() {
    return largeur;
  }

  public int getHeight() {
    return hauteur;
  }

  // gère l'affichage, notamment en mode debug
  public String getAppearance(Bestiole c) {
    if (!debugView)
      return infosBestioles.get(c).string;
    else {
      PrivateData data = infosBestioles.get(c);
      if (data.direction == Bestiole.Direction.NORD)
        return "^";
      else if (data.direction == Bestiole.Direction.SUD)
        return "v";
      else if (data.direction == Bestiole.Direction.EST)
        return ">";
      else
        return "<";
    }
  }

  public void toggleDebug() {
    this.debugView = !this.debugView;
  }

  private boolean inBounds(int x, int y) {
    return (x >= 0 && x < largeur && y >= 0 && y < hauteur);
  }

  private boolean inBounds(Point coord) {
    return inBounds(coord.x, coord.y);
  }

  // retourne la nouvelle direction après rotation
  private Bestiole.Direction tourner(Bestiole.Direction dir) {
    if (dir == Bestiole.Direction.NORD)
      return Bestiole.Direction.EST;
    else if (dir == Bestiole.Direction.SUD)
      return Bestiole.Direction.OUEST;
    else if (dir == Bestiole.Direction.EST)
      return Bestiole.Direction.SUD;
    else
      return Bestiole.Direction.NORD;
  }

  private Point caseEnFace(Point coord, Bestiole.Direction dir) {
    if (dir == Bestiole.Direction.NORD)
      return new Point(coord.x, coord.y - 1);
    else if (dir == Bestiole.Direction.SUD)
      return new Point(coord.x, coord.y + 1);
    else if (dir == Bestiole.Direction.EST)
      return new Point(coord.x + 1, coord.y);
    else
      return new Point(coord.x - 1, coord.y);
  }

  private Info recupererInfo(PrivateData data, Class original) {
    Bestiole.Voisin[] neighbors = new Bestiole.Voisin[4];
    Bestiole.Direction d = data.direction;
    boolean[] neighborThreats = new boolean[4];
    for (int i = 0; i < 4; i++) {
      neighbors[i] = getVoisin(caseEnFace(data.coord, d), original);
      if (neighbors[i] == Bestiole.Voisin.AUTRE) {
        Point p = caseEnFace(data.coord, d);
        PrivateData oldData = infosBestioles.get(grille[p.x][p.y]);
        neighborThreats[i] = d == tourner(tourner(oldData.direction));
      }
      d = tourner(d);
    }
    return new Info(neighbors, data.direction, neighborThreats);
  }

  private Bestiole.Voisin getVoisin(Point p, Class original) {
    if (!inBounds(p))
      return Bestiole.Voisin.MUR;
    else if (grille[p.x][p.y] == null)
      return Bestiole.Voisin.RIEN;
    else if (grille[p.x][p.y].getClass() == original)
      return Bestiole.Voisin.MEME;
    else
      return Bestiole.Voisin.AUTRE;
  }

  public void update() {
    simulationSteps++;
    Bestiole[] bestioles = infosBestioles.keySet().toArray(new Bestiole[0]);
    Collections.shuffle(Arrays.asList(bestioles));

    // Bestioles qui ne peuvent plus être infectées sur ce tour
    // (bestiole déjà infectée ou a déjà sauté)
    Set<Bestiole> locked = new HashSet<Bestiole>();

    for (int i = 0; i < bestioles.length; i++) {
      Bestiole next = bestioles[i];
      PrivateData data = infosBestioles.get(next);
      if (data == null) {
        continue;
      }
      boolean hadHopped = data.justHopped;
      data.justHopped = false;
      Point p = data.coord;
      Point p2 = caseEnFace(p, data.direction);

      Bestiole.Action move = next.getAction(recupererInfo(data, next.getClass()));
      if (move == Bestiole.Action.GAUCHE)
        data.direction = tourner(tourner(tourner(data.direction)));
      else if (move == Bestiole.Action.DROITE)
        data.direction = tourner(data.direction);
      else if (move == Bestiole.Action.SAUTER) {
        if (inBounds(p2) && grille[p2.x][p2.y] == null) {
          grille[p2.x][p2.y] = grille[p.x][p.y];
          grille[p.x][p.y] = null;
          data.coord = p2;
          locked.add(next); // succès du saut, la bestiole ne pourra pas être infectée sur ce tour
          data.justHopped = true;
        }
      } else if (move == Bestiole.Action.INFECTER) {
        if (inBounds(p2) && grille[p2.x][p2.y] != null && grille[p2.x][p2.y].getClass() != next.getClass()
            && !locked.contains(grille[p2.x][p2.y]) && (hadHopped || Math.random() >= AVANTAGE_SI_SAUT)) {
          Bestiole other = grille[p2.x][p2.y];
          PrivateData oldData = infosBestioles.get(other);
          String c1 = other.getClass().getName();
          comptageBestioles.put(c1, comptageBestioles.get(c1) - 1);
          String c2 = next.getClass().getName();
          comptageBestioles.put(c2, comptageBestioles.get(c2) + 1);
          infosBestioles.remove(other);
          try {
            grille[p2.x][p2.y] = instancierBestiole(next.getClass());
            locked.add(grille[p2.x][p2.y]);
          } catch (Exception e) {
            throw new RuntimeException("" + e);
          }
          infosBestioles.put(grille[p2.x][p2.y], oldData);
          oldData.justHopped = false;
        }
      }
    }
    updateColorString();
  }

  public void updateColorString() {
    for (Bestiole next : infosBestioles.keySet()) {
      infosBestioles.get(next).color = next.getCouleur();
      infosBestioles.get(next).string = next.toString();
    }
  }

  public Set<Map.Entry<String, Integer>> getCounts() {
    return Collections.unmodifiableSet(comptageBestioles.entrySet());
  }

  public int getSimulationCount() {
    return simulationSteps;
  }

  private class PrivateData {
    public Point coord;
    public Bestiole.Direction direction;
    public Color color;
    public String string;
    public boolean justHopped;

    public PrivateData(Point pt, Bestiole.Direction dir) {
      this.coord = pt;
      this.direction = dir;
    }

    public String toString() {
      return coord + " " + direction;
    }
  }

  private static class Info implements BestioleInfo {
    private Bestiole.Voisin[] neighbors;
    private Bestiole.Direction direction;
    private boolean[] neighborThreats;

    public Info(Bestiole.Voisin[] neighbors, Bestiole.Direction dir, boolean[] neighborThreats) {
      this.neighbors = neighbors;
      this.direction = dir;
      this.neighborThreats = neighborThreats;
    }

    public Bestiole.Voisin getEnFace() {
      return neighbors[0];
    }

    public Bestiole.Voisin getDerriere() {
      return neighbors[2];
    }

    public Bestiole.Voisin getAGauche() {
      return neighbors[3];
    }

    public Bestiole.Voisin getADroite() {
      return neighbors[1];
    }

    public Bestiole.Direction getDirectionActuelle() {
      return direction;
    }

    public boolean MenaceEnFace() {
      return neighborThreats[0];
    }

    public boolean MenaceDerriere() {
      return neighborThreats[2];
    }

    public boolean MenaceAGauche() {
      return neighborThreats[3];
    }

    public boolean MenaceADroite() {
      return neighborThreats[1];
    }
  }
}
