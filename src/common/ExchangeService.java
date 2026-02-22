package common;

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface ExchangeService extends Remote {

    List<FinancialInstrument> getMarketSnapshot() throws RemoteException;

    //Register a client and get a client ID. Returns clientId.
    int registerClient() throws RemoteException;

    List<Order> getBidOrders(String symbol) throws RemoteException;


    List<Order> getAskOrders(String symbol) throws RemoteException;

    String placeBuyOrder(int clientId, String symbol, double price, double quantity) throws RemoteException;

    String placeSellOrder(int clientId, String symbol, double price, double quantity) throws RemoteException;

    //Returns all trades for a given symbol on a given day
    List<Trade> getTradesForDay(String symbol, String day) throws RemoteException;

    ClientAccount getClientAccount(int clientId) throws RemoteException;

    int getTcpPort() throws RemoteException;
}
