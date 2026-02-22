package server;

import common.*;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;

public class ExchangeServiceImpl extends UnicastRemoteObject implements ExchangeService {

    private final MarketEngine engine;
    private final int tcpPort;

    public ExchangeServiceImpl(MarketEngine engine, int tcpPort) throws RemoteException {
        super();
        this.engine = engine;
        this.tcpPort = tcpPort;
    }

    @Override
    public List<FinancialInstrument> getMarketSnapshot() throws RemoteException {
        return engine.getSnapshot();
    }

    @Override
    public int registerClient() throws RemoteException {
        return engine.registerClient();
    }

    @Override
    public List<Order> getBidOrders(String symbol) throws RemoteException {
        return engine.getBidOrders(symbol);
    }

    @Override
    public List<Order> getAskOrders(String symbol) throws RemoteException {
        return engine.getAskOrders(symbol);
    }

    @Override
    public String placeBuyOrder(int clientId, String symbol, double price, double quantity) throws RemoteException {
        return engine.placeBuyOrder(clientId, symbol, price, quantity);
    }

    @Override
    public String placeSellOrder(int clientId, String symbol, double price, double quantity) throws RemoteException {
        return engine.placeSellOrder(clientId, symbol, price, quantity);
    }

    @Override
    public List<Trade> getTradesForDay(String symbol, String day) throws RemoteException {
        return engine.getTradesForDay(symbol, day);
    }

    @Override
    public ClientAccount getClientAccount(int clientId) throws RemoteException {
        return engine.getClientAccount(clientId);
    }

    @Override
    public int getTcpPort() throws RemoteException {
        return tcpPort;
    }
}
