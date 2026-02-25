
## Digitalna berza (RMI + TCP Sockets)

## Arhitektura sistema
```
┌─────────────────────────────────────────────────────┐
│                      SERVER                         │
│                                                     │
│  ┌───────────────┐  ┌─────────────┐  ┌──────────┐   │
│  │ExchangeService│  │MarketEngine │  │TcpServer │   │
│  │  (RMI/Java)   │◄►│ (OrderBook, │◄►│port:5000 │   │
│  │  port: 1099   │  │  Matching)  │  │          │   │
│  └───────────────┘  └─────────────┘  └──────────┘   │
│                           │                         │
│                    ┌──────▼──────┐                  │
│                    │TradeArchiver│                  │
│                    │(File/Thread)│                  │
│                    └─────────────┘                  │
└─────────────────────────────────────────────────────┘
          RMI (port 1099) │ TCP (port 5000)
                          │
┌─────────────────────────┴───────────────────────────┐
│                     KLIJENTI                        │
│                                                     │
│  ┌─────────────────┐    ┌──────────────────────┐    │
│  │  ExchangeClient │    │  AutoTraderLauncher  │    │
│  │  (interaktivni) │    │  (5 bot klijenata)   │    │
│  └─────────────────┘    └──────────────────────┘    │

```
## Struktura projekta

```
berza/
├── src/
│   └── berza/
│       ├── common/          ← Deljene klase 
│       │   ├── FinancialInstrument.java
│       │   ├── Order.java
│       │   ├── Trade.java
│       │   ├── MarketUpdate.java
│       │   ├── ClientAccount.java
│       │   └── ExchangeService.java   ← RMI interfejs
│       ├── server/
│       │   ├── ExchangeServer.java    ← MAIN klasa servera
│       │   ├── ExchangeServiceImpl.java
│       │   ├── MarketEngine.java
│       │   ├── OrderBook.java
│       │   ├── TcpMarketServer.java
│       │   ├── TcpClientHandler.java
│       │   ├── TradeArchiver.java
│       │   └── SimulationClock.java
│       └── client/
│           ├── ExchangeClient.java    ← MAIN klasa klijenta
│           ├── AutoTraderLauncher.java ← MAIN klasa botova
│           ├── AutoTrader.java
│           ├── TcpMarketReceiver.java
│           ├── MarketDisplay.java
│           └── ConsoleColors.java
├── bin/          

```

## Pokretanje u Eclipse-u

1. **Novi Java projekt** → uvezi sve fajlove iz `src/`
2. **Run → Run Configurations** → kreirati 3 konfiguracije:
   - `ExchangeServer` → main class: `berza.server.ExchangeServer`
   - `ExchangeClient` → main class: `berza.client.ExchangeClient`  
   - `AutoTraderLauncher` → main class: `berza.client.AutoTraderLauncher`
3. Pokrenuti redom: Server → AutoTraderLauncher → ExchangeClient

## Implementirani zahtevi

| Zahtev | Status | Opis |
|--------|--------|------|
| RMI servis | `ExchangeService` interfejs, `ExchangeServiceImpl` implementacija |
| TCP Socket server  | `TcpMarketServer` + `TcpClientHandler` |
| N ≥ 12 instrumenata  | 15 kripto valuta sa CoinMarketCap cenama |
| Market snapshot (RPC)  | `getMarketSnapshot()` |
| Realno-vremenski feed  | TCP objekti, u-mestu update konzole |
| Višestruka pretplata  | Klijent bira simbole, server filtrira |
| Bojeni prikaz ▲▼  | ANSI boje, zeleno/crveno, +/- znaci |
| In-place row update | ANSI escape sekvence za kursor |
| Bid/Ask lista (RPC) | Sortirano - Bid desc, Ask asc |
| Nalog za kupovinu/prodaju | `placeBuyOrder`/`placeSellOrder` |
| Provjera sredstava | Provjera gotovine i holdings-a |
| Order matching  | `OrderBook.matchOrders()` |
| TCP obaveštenje o tradeovima  | `TRADE_EXECUTED` broadcast |
| Trade arhiviranje | Posebna nit, CSV fajlovi |
| Istorija tradeova (RPC) | `getTradesForDay(symbol, day)` |
| Automatski bots  | 5 AutoTrader instanci |
| Simulirano vreme   | 1s real = 60s sim (1min real = 1h sim) |
| Periodni price update  | Scheduler svakih 2 sekunde |

## Simulovano vreme

- `SimulationClock`: 1 realna sekunda = 60 simulacijskih sekundi
- Znači: 1 realni minut = 1 simulacijski sat
- Simulacija počinje od: 2025-01-01 09:00:00


## Cene instrumenata

| Simbol | Naziv | Cena (USD) |

{"BTC",  "Bitcoin",          67383.0},

{"ETH",  "Ethereum",          1946.0},

{"BNB",  "BNB",                614.0},

{"SOL",  "Solana",             83.0},

{"XRP",  "XRP",                  1.3},

{"ADA",  "Cardano",              0.2},

{"AVAX", "Avalanche",            8.8},

{"DOGE", "Dogecoin",             0.38},

{"DOT",  "Polkadot",             1.3},

{"MATIC","Polygon",              0.52},

{"LINK", "Chainlink",           8.7},

{"UNI",  "Uniswap",             3.4},

{"LTC",  "Litecoin",            53.0},

{"ATOM", "Cosmos",               2.2},

{"NEAR", "NEAR Protocol",        1.0},

