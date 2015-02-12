package experiments.experiment1.hosts.router1

import com.sun.org.apache.xpath.internal.operations.Bool
import common.utils.Utils

import javax.rmi.CORBA.Util

/**
 * Ein IPv4-Router.<br/>
 * Nur als Ausgangspunkt für eigene Implementierung zu verwenden!<br/>
 * Verwendet UDP zur Verteilung der Routinginformationen.
 *
 */
class Router1 {

    //========================================================================================================
    // Vereinbarungen ANFANG
    //========================================================================================================

    /** Der Netzwerk-Protokoll-Stack */
    experiments.experiment1.stack.Stack stack

    /** Konfigurations-Objekt */
    ConfigObject config

    /** Stoppen der Threads wenn false */
    Boolean run = true

    /** Tabelle der IP-Adressen und UDP-Ports der Nachbarrouter */
    /*  z.B. [["1.2.3.4", 11],["5,6,7.8", 20]]
     */
    List<List> neighborTable

    /** Eine Arbeitskopie der Routingtabelle der Netzwerkschicht */
    List routingTable

    /** count Var */

    Thread toc1
    Thread toc2

    final int COUNTDOWN_RT = 10
    private int c_toc1 = COUNTDOWN_RT
    private int c_toc2 = COUNTDOWN_RT

    //========================================================================================================
    // Methoden ANFANG
    //========================================================================================================

    //------------------------------------------------------------------------------
    /**
     * Start der Anwendung
     */
    static void main(String[] args) {
        // Router-Klasse instanziieren
        Router1 application = new Router1()
        // und starten
        application.router()
    }
    //------------------------------------------------------------------------------

    //------------------------------------------------------------------------------
    /**
     * Einfacher IP-v4-Forwarder.<br/>
     * Ist so schon funktiionsfähig, da die Wegewahl im Netzwerkstack erfolgt<br/>
     * Hier wird im Laufe des Versuchs ein Routing-Protokoll implementiert.
     */
    void router() {

        // Konfiguration holen
        config = Utils.getConfig("experiment1", "router1")

        // ------------------------------------------------------------

        // Netzwerkstack initialisieren
        stack = new experiments.experiment1.stack.Stack()
        stack.start(config)

        // ------------------------------------------------------------

        neighborTable = config.neighborTable

        // Thread zum Empfang von Routinginformationen erzeugen
        Thread.start { receiveFromNeigbor() }

        // ------------------------------------------------------------

        Utils.writeLog("Router1", "router1", "startet", 1)
        toc1 = new Thread()
        toc2 = new Thread()
        toc1.start { timeOutCounter1() }
        toc2.start { timeOutCounter2() }

        Thread.start {
            while (run) {
                sleep(4000)
                Boolean changed = false

                List<List> rt = stack.getRoutingTable()
                if (c_toc1 <= 0) {
                    for (def mL in rt) {
                        if (mL.get(2) == neighborTable[0].get(0).toString()) {
                            Utils.writeLog("Router", "timeoutC", "time out erreicht für ${neighborTable[1].get(0).toString()}", 11)
                            mL.set(4, "10")// Entfernung auf unendlich
                            changed = true
                        }
                    }
                }


                if (c_toc2 <= 0) {
                    for (def mL in rt) {
                        if (mL.get(2) == neighborTable[1].get(0).toString()) {
                            Utils.writeLog("Router", "timeoutC", "time out erreicht für ${neighborTable[1].get(0).toString()}", 11)
                            mL.set(4, "10")// Entfernung auf unendlich
                            changed = true
                        }
                    }
                }
                if (changed) stack.ip.setSortingRoutingTable(rt)

            }
        }


        while (run) {
            // Periodisches Versenden von Routinginformationen
            sleep(config.periodRInfo)
            sendPeriodical()
        }
    }

    // ------------------------------------------------------------

    /**
     * Wartet auf Empfang von Routinginformationen
     *
     */

    void receiveFromNeigbor() {
        /** IP-Adresse des Nachbarrouters */
        String iPAddr

        /** UDP-Portnummer des Nachbarrouters */
        int port

        /** Anschlussport und Dummy */
        String lp, d1

        /** Empfangene Routinginformationen */
        String rInfo

        while (run) {
            // Auf UDP-Empfang warten
            (iPAddr, port, rInfo) = stack.udpReceive()


            Utils.writeLog("Router", "rcvFrNb", "empfange Paket", 11)

            switch (iPAddr) {
                case neighborTable[0]: c_toc1 = COUNTDOWN_RT
                    break
                case neighborTable[1]: c_toc2 = COUNTDOWN_RT
                    break
                default: break
            }

            List iInfo = rInfo.tokenize()

            // Jetzt aktuelle Routingtablle holen:
            List<List> rt = stack.getRoutingTable()

            // Kleiner Trick um den Linkport zu erfahren, von dem aus die rInfo kam
            (lp, d1) = stack.ip.findNextHop(iPAddr)

            //Utils.getNetworkId(iPAddr,"255.255.255.0")
            iInfo.each { m ->
                String mInfo = m as String
                List mInfoL = mInfo.tokenize(',')
                Boolean ownIP = false
                String HopIP = mInfoL.get(2).toString()
                stack.ip.ownIpAddrs.each { ip ->
                    String ipString = ip.value
                    if (HopIP == ipString) {
                        ownIP = true
                    }
                }
                if (!ownIP) {
                    String Zielnetz = mInfoL.get(0).toString()
                    String Netzmaske = mInfoL.get(1).toString()
                    Integer Kosten = (mInfoL.get(3).toInteger() + 1) >= 10 ? 10 : mInfoL.get(3).toInteger() + 1

                    // existiert ein entsprechender Eintrag in meiner RoutingTable?
                    Boolean foundOwnZiel = false
                    Boolean foundOwnHop = false
                    Boolean cheapestConn = false
                    rt.each { rtEntry ->
                        if (rtEntry.get(0) == Zielnetz && rtEntry.get(1) == Netzmaske) {
                            foundOwnZiel = true // Ja!
                            if (rtEntry.get(2) == iPAddr) { // die Router-IP-Adressen gleichen sich
                                foundOwnHop = true
                                if (rtEntry.get(4).toString().toInteger() < 10) {
                                    rtEntry.set(4, Kosten.toString())
                                    // trage die neuen Kosten ein
                                    Utils.writeLog("Router2", "receiveFromNb", "verändere Kosten: ${Zielnetz + " " + Netzmaske + " " + iPAddr + " " + Kosten.toString()}", 11)
                                }
                            }
                            if (rtEntry.get(4) == "1") cheapestConn = true
                        }
                    }
                    // Wenn beide variablen true sind, dann gibt es einen Eintrag in der RT, der genau über den src-Router
                    // läuft, es darf also keinen neuen Eintrag dieser Art geben in der RT
                    if (!(foundOwnZiel && foundOwnHop) && !cheapestConn) {
                        // falls der Ziel-Eintrag in Routingtabelle  noch gar nicht interessiert, füge ihn hinzu
                        rt.add([Zielnetz, Netzmaske, iPAddr, lp, Kosten.toString()])
                        Utils.writeLog("Router1", "receiveFromNb", "füge hinzu: ${Zielnetz + " " + Netzmaske + " " + iPAddr + " " + Kosten.toString()}", 11)
                    }
                }
            }
            stack.ip.setSortingRoutingTable(rt)
        }
    }

// ------------------------------------------------------------

/** Periodisches Senden der Routinginformationen */
    void sendPeriodical() {
        // Paket mit Routinginformationen packen

        // Routingtabelle holen
        routingTable = stack.getRoutingTable()

        // extrahieren von Information, dann rInfo als Zeichenkette
        String rInfo = "";
        List routingEntry;
        for (int i = 0; i < routingTable.size(); i++) {
            routingEntry = routingTable.get(i)

            if (i == routingTable.size() - 1) {
                rInfo = rInfo + routingEntry.get(0) + "," + routingEntry.get(1) + "," + routingEntry.get(2) + "," + routingEntry.get(4)
            } else {
                rInfo = rInfo + routingEntry.get(0) + "," + routingEntry.get(1) + "," + routingEntry.get(2) + "," + routingEntry.get(4) + " "
            }
        }

        // Zum Senden uebergeben
        sendToNeigbors(rInfo)
    }

// ------------------------------------------------------------

/** Senden von Routinginformationen an alle Nachbarrouter
 *
 * @param rInfo - Routing-Informationen
 */

    void sendToNeigbors(String rInfo) {
        // rInfo an alle Nachbarrouter versenden
        for (List neigbor in neighborTable) {
            stack.udpSend(dstIpAddr: neigbor[0], dstPort: neigbor[1],
                    srcPort: config.ownPort, sdu: rInfo)
        }
    }
//------------------------------------------------------------------------------

    void timeOutCounter1() {
        while (run) {
            sleep(1000)
            while (c_toc1 > 0) {
                sleep(1000)
                c_toc1--
            }
        }
    }

    void timeOutCounter2() {
        while (run) {
            sleep(1000)
            while (c_toc2 > 0) {
                sleep(1000)
                c_toc2--
            }
        }
    }

}
