/*
 * Okno programu serwera
 * Zdaję sobie sprawę z tego że łamie to wzorzec MVC, naprawdę zabrakło mi czasu
 * choć zacząłem już implementację w porządny sposób czego dowodem jest klasa adaptera
 * 
 */

package dalilaserver;

import HostData.DataSource;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.io.ObjectOutputStream;
import static java.lang.System.exit;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;


/**
 * klasa okna serwera dzidzicząca po JFrame
 * @author zadruzyn
 */
public class DalilaFrame extends JFrame
{
   private static final int NOFOUND = -1;
   //private ArrayList<HostBox> hosts;
   private HashMap<Integer,HostBox> hosts;
   private HashSet openedHosts;
   private JTable hostsTable;
   private JTabbedPane pane;
   private JScrollPane hoststablePane;
   private JButton closeBtn;
   private JButton refreshBtn;
   private JButton removeBtn;
   private final Dimension screenSize;
   private JTableAdapter tableAdapter;
   
   /**
    * Kontruktor. Pirwszy tyr jest tylko dlatego że ładuję inny motyw
    * który nie zawsze musi być w VM usera
    * @param title tytuł okna
    * @throws IOException 
    */
   public DalilaFrame(String title) throws IOException {
      super(title);
      try
      {
         UIManager.setLookAndFeel("com.sun.java.swing.plaf.nimbus.NimbusLookAndFeel");
      } catch (UnsupportedLookAndFeelException ex)
      {
         Logger.getLogger(DalilaFrame.class.getName()).log(Level.SEVERE, null, ex);
      } catch (ClassNotFoundException ex)
      {
         Logger.getLogger(DalilaFrame.class.getName()).log(Level.SEVERE, null, ex);
      } catch (InstantiationException ex)
      {
         Logger.getLogger(DalilaFrame.class.getName()).log(Level.SEVERE, null, ex);
      } catch (IllegalAccessException ex)
      {
         Logger.getLogger(DalilaFrame.class.getName()).log(Level.SEVERE, null, ex);
      }
      setLayout(new BorderLayout());
      this.screenSize = new Dimension(Toolkit.getDefaultToolkit().getScreenSize());
      int x = (int)screenSize.getWidth()/2;
      int y = (int)screenSize.getHeight()/2;
      this.setSize(x,y);
      this.setLocation(x-x/2, y-y/2);
      this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

      hosts = new HashMap<>();
      openedHosts = new HashSet();
      this.initComp();
      this.startServer(8189);
   }
   /**
    * Inicjalizacja początkowa wszystkich komponentów
    * Ustawienie listenerów
    */
   private void initComp(){
      hostsTable = new JTable();
      updateHosts();
      hoststablePane = new JScrollPane(hostsTable);
      this.setLayout(new BorderLayout());
      JPanel west = new JPanel();
      west.add(hoststablePane);
      this.add(west, BorderLayout.WEST);
      this.pane = new JTabbedPane();

      this.add(pane, BorderLayout.CENTER);
      

      
      JPanel south = new JPanel();
      this.closeBtn = new JButton("Zamknij");
      this.removeBtn =  new JButton("Usuń");
      this.refreshBtn =  new JButton("Odśwież");
      south.setLayout(new FlowLayout());
      south.add(refreshBtn);
      south.add(removeBtn);
      south.add(closeBtn);
      
      this.add(south, BorderLayout.SOUTH);
      tableAdapter = new JTableAdapter();
      
      hostsTable.addMouseListener(new MouseAdapter()
         {
            @Override
            public void mouseClicked(MouseEvent evt)
            {
               hostSelected(evt);
            }
         });
      closeBtn.addMouseListener(new MouseAdapter()
         {
            @Override
            public void mouseClicked(MouseEvent evt)
            {
               closeBtnClicked(evt);
            }
         });
      pane.addMouseListener(new MouseAdapter()
         {
            @Override
            public void mouseClicked(MouseEvent evt)
            {
               tabSelected(evt);
            }
         });
      /**
       * nic nie jest zaznaczone - ArrayIndexOutOfBoundsException
       */
      removeBtn.addMouseListener(new MouseAdapter()
         {
            @Override
            public void mouseClicked(MouseEvent evt)
            {
               try{
                  removeHost(evt, (int)hostsTable.getModel().getValueAt(
                          hostsTable.getSelectedRow(), 0));
               }catch(ArrayIndexOutOfBoundsException ex){}
            }
         });
      refreshBtn.addMouseListener(new MouseAdapter()
         {
            @Override
            public void mouseClicked(MouseEvent evt)
            {
               try{
                  refreshHost(evt, (int)hostsTable.getModel().getValueAt(
                        hostsTable.getSelectedRow(), 0));
               }catch(ArrayIndexOutOfBoundsException ex){}
            }
         });

   }
   /**
    * Update listy klientów widocznych dla użytkownika
    */
   private void updateHosts(){
      Object columnNames[] = {"ID", "Adres IP", "OS"};
      Object rowData[][] = new Object[hosts.size()][];
      int i =0;
      for(HostBox data: hosts.values()) {
         rowData[i++] = 
                 new Object[]{data.getID(), data.getIP(),data.getHostData().getOsName()};
      }
      
      DefaultTableModel model = new DefaultTableModel(rowData,columnNames){
         @Override
         public boolean isCellEditable(int row, int column) {
            return false;
         }
      };
      
      hostsTable.setModel(model);
      
   }
   /**
    * Obsługa zdarzenia dla kliknięcia w wiersz z hostem
    * @param evt klikniecie myszą
    */
   private void hostSelected(MouseEvent evt)
   {
      int selectedID = (int)hostsTable.getModel().getValueAt(hostsTable.getSelectedRow(), 0);
      if(!openedHosts.contains(selectedID)){
         HostBox tmp = hosts.get(selectedID);
         if(tmp!=null){
            pane.add(tmp.getIP(), new HostInfoPanel(tmp));
            openedHosts.add(selectedID);
         }
      }else{
         this.pane.setSelectedIndex(hostsTable.getSelectedRow());
      }
      
   }
   /**
    * żaden host nie jest otworzony - IllegalArgumentException
    * @param evt 
    */
   private void tabSelected(MouseEvent evt){
      try{
         hostsTable.setRowSelectionInterval(pane.getSelectedIndex(), pane.getSelectedIndex()); 
      }catch(IllegalArgumentException ex){}
   }
   
   private void closeBtnClicked(MouseEvent evt)
   {
      exit(0);
   }
   
   public synchronized void addHost(DataSource data, String ip, Socket s){
      int tmp = getPosOfHost(ip);
      if(tmp!=NOFOUND){
         hosts.get(tmp).renewValues(data);
      }else {
         HostBox tmpH = new HostBox(data,ip,s);
         hosts.put(tmpH.getID(), tmpH);
      }
      this.updateHosts();
      if(openedHosts.contains(tmp)){
         int selected = pane.getSelectedIndex();
         pane.remove(tmp);
         pane.add(hosts.get(tmp).getIP(), new HostInfoPanel(hosts.get(tmp)));
         pane.setSelectedIndex(selected);
      }
   }
   /**
    * usuwanie klienta z listy
    * @param evt kliknięcie myszą
    * @param id ID przekazane z listenera
    */
   private synchronized void removeHost(MouseEvent evt, int id){
         if(hosts.containsKey(id)){
            this.pane.remove(hostsTable.getSelectedRow());
            this.openedHosts.remove(id);
            this.hosts.remove(id);
            this.updateHosts();
         }
      
   }
   /**
    * odpalenie wątku serwera
    * @param port port na którym będzie działać (hardcode 8189)
    * @throws IOException 
    */
   private void startServer(int port) throws IOException{
      Thread t = new Thread(new Server(port,this));
      t.start();
   }
   /**
    * wysyłanie przez socket info do klienta z prośbą o ponowne przesłanie danyc 
    * niestety nie zostało zaimplementowane do końca
    * a postronie klienta wcale
    * @param evt
    * @param id 
    */  
   private void refreshHost(MouseEvent evt, int id){
      if(hosts.containsKey(id)){
         try {
            ObjectOutputStream out= new ObjectOutputStream(
                    hosts.get(id).getSocket().getOutputStream());
            out.writeObject("pass");
            out.close();
            
         } catch (IOException ex) {
            Logger.getLogger(DalilaFrame.class.getName()).log(Level.SEVERE, null, ex);
         }
      }
   }
   /**
    * Helperowa metoda do odzyskiwania indywidualnego ID klienta na podstawie
    * adresu IP
    * Niestety na potrzeby projektu uznałem że IP = unikalny klient
    * 
    * @param ip
    * @return 
    */
   private int getPosOfHost(String ip){
      for(HostBox data: hosts.values())
         if(data.getIP().equals(ip)) return data.getID();
      return NOFOUND;
   }
}
