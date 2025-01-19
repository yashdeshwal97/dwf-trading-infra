package exchanges.ftx

import crypto.rest.ftx.service.FTXBaseService
import exchanges.services.IMarketDataService
import exchanges.Exchange

class FTXMarketDataService(exchange: Exchange) : FTXBaseService(exchange), IMarketDataService