package app.myliberty.easy_records.locations.business.retail.stock

val stockEntryFifoComparator: Comparator<StockEntryEntity> =
    compareBy<StockEntryEntity> { stockEntryEntity: StockEntryEntity -> stockEntryEntity.priority }
        .thenBy(nullsLast()) { stockEntryEntity: StockEntryEntity -> stockEntryEntity.activatedOn }
        .thenBy(nullsLast()) { stockEntryEntity: StockEntryEntity -> stockEntryEntity.createdOn }
