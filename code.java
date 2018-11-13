class RegionsVM : BaseActVM<RegionsAct>() {

    @Inject
    lateinit var filtersInteractor: IFiltersInteractor

    @Inject
    lateinit var analyticsManager: AnalyticsManager

    var title: String = EventsApp.appContext.getString(R.string.choose_regions)

    var hint: String = EventsApp.appContext.getString(R.string.search_region)

    private val itemType =
            Type<ItemFilterRegionBinding>(R.layout.item_filter_region)

    private val regions = mutableListOf<EventCountry>()
    private val codes = mutableListOf<String>()

    private val listForDisplaying = ObservableArrayList<ItemRegionVM>()

    var adapter: ObservableField<LastAdapter> = ObservableField(
            LastAdapter(listForDisplaying, BR.vm)
                    .map<ItemRegionVM>(itemType))

    override fun attachView(view: RegionsAct, bn: Bundle?, intent: Intent?) {
        super.attachView(view, bn, intent)
        EventsAppGraph.addEventsComponent().inject(this)
    }

    override fun onViewAttached() {
        super.onViewAttached()
        loadData()
        initSearchView()
        analyticsManager.sendEvent(ViewScreenEvent("filter_region"))
    }

    override fun detachView() {
        super.detachView()
        EventsAppGraph.removeEventsComponent()
    }

    fun onBackClick(v: View) {
        view?.onBackPressed()
        analyticsManager.sendEvent(ClickButtonEvent("filter_region", "back"))
    }

    private fun onItemChecked(code: String, checked: Boolean) {
        when (checked) {
            true -> {
                filtersInteractor.addSelectedRegion(code)
                codes.add(code)
            }
            false -> {
                filtersInteractor.removeSelectedRegion(code)
                codes.remove(code)
            }
        }
    }

    private fun loadData() {
        Single.zip(
                filtersInteractor.getRegionsFilter(),
                filtersInteractor.getSelectedRegionsCodes(),
                BiFunction { regions_: MutableList<EventCountry>, codes_: HashSet<String> ->
                    codes.apply {
                        clear()
                        addAll(codes_)
                    }
                    regions.apply {
                        clear()
                        addAll(regions_)
                    }
                })
                .subscribe({

                    listForDisplaying.apply {
                        clear()
                        addAll(getRegionsFilterVms(regions))
                    }

                }, {
                    report(it)
                }).clearWith(disposables)
    }

    private fun initSearchView() {
        view?.binding?.edtSearch?.let {
            RxTextView.textChanges(it)
                    .debounce(100, TimeUnit.MILLISECONDS)
                    .switchMap { charSequence ->
                        Observable.fromCallable {
                            regions.filter { it.name.contains(charSequence, true) }
                        }
                    }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe({
                        listForDisplaying.apply {
                            clear()
                            addAll(getRegionsFilterVms(it))
                        }
                    }, {
                        report(it)
                    }).clearWith(disposables)
        }
    }

    private fun getRegionsFilterVms(regions: List<EventCountry>): List<ItemRegionVM> {
        return regions.map {
            ItemRegionVM(it.code, it.name, this::onItemChecked, codes.contains(it.code))
        }
    }
}
