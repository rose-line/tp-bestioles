// Cette interface définit un ensemble de méthodes qui permettent
// de connaître l'état d'une bestiole. N'y touchez pas.

package fr.pgah.bestioles;

public interface BestioleInfo {
    public Bestiole.Voisin getEnFace();
    public Bestiole.Voisin getDerriere();
    public Bestiole.Voisin getAGauche();
    public Bestiole.Voisin getADroite();
    public Bestiole.Direction getDirectionActuelle();
    public boolean MenaceEnFace();
    public boolean MenaceDerriere();
    public boolean MenaceAGauche();
    public boolean MenaceADroite();
}