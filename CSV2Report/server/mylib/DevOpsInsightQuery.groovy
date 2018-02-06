import groovy.json.JsonBuilder
import groovy.json.JsonOutput

class DevOpsInsightQueryBuilder {

    List<Filter> filters = []
    List<Group> aggregations = []
    int size = 10

    void size(final int size) {
        this.size = size
    }

    void filter(@DelegatesTo(Filter) final Closure config) {
        final Filter filter = new Filter()
        filter.with config
        filters.add(filter)
    }

    static class Filter {
        String operation
        String column
        String value

        protected void operation(final String operation) { this.operation = operation }
        void column(final String column) { this.column = column }
        void matches(final String value) {
            operation('term')
            this.value = value
        }

        void exists() {
            operation('exists')
        }
    }

    static class Group {
        String column
        String interval
        String name
        String aggregation

        void aggregation(final String aggregation) { this.aggregation = aggregation }
        void by(final String interval) { this.interval = interval }
        void column(final String column) { this.column = column }
        void name(final String name) { this.name = name }

        def determineAggregation() {
            aggregation?: interval ? 'date_histogram' : 'terms'
        }
    }

    void group(final String name, @DelegatesTo(Group) final Closure config) {
        size(0)
        final Group group = new Group()
        group.name(name)
        group.with config
        aggregations.add(group)
    }



    static def build(@DelegatesTo(DevOpsInsightQueryBuilder) final Closure config) {
        println 'inside static build'
        final DevOpsInsightQueryBuilder builder = new DevOpsInsightQueryBuilder()
        builder.with config
        builder.build()
    }

    def build_orig() {

        def query = [
                size: size,
                query: [
                        bool: [
                                filter: filters.collect {
                                    switch (it.operation) {
                                        case 'term':
                                            [ (it.operation): [ (it.column): it.value ] ]
                                            break
                                        case 'exists':
                                            [ (it.operation): [ field: it.column ] ]
                                            break
                                    }
                                }

                        ]
                ],

        ]

        JsonBuilder json = new JsonBuilder(query)
        JsonOutput.prettyPrint(json.toString())
    }
    def build() {

        def jsonBuilder = new JsonBuilder()
        jsonBuilder {
            size size
            query {
                bool {
                    filter filters.collect {
                        switch (it.operation) {
                            case 'term':
                                [(it.operation): [(it.column): it.value]]
                                break
                            case 'exists':
                                [(it.operation): [field: it.column]]
                                break
                        }
                    }
                    this.buildAggregations(delegate, 0)
                }
            }
        }

        jsonBuilder.toPrettyString()
    }

    def buildAggregations(def parent, int index) {
        if (aggregations.size() <= index) {
            return
        }

        def agg = aggregations.get(index)
        def aggType = agg.determineAggregation()

        def jsonBuilder = new JsonBuilder()

        parent.aggregation jsonBuilder {
            "$agg.name" {
                this.buildAggregation(delegate, agg, aggType)
                this.buildAggregations(delegate, ++index)
            }
        }
    }

    def buildAggregation(def parent, def agg, def aggType) {
        def aggregation
        switch (aggType) {
            case 'date_histogram':
                aggregation = [
                        field: agg.name,
                        interval: agg.interval,
                        format: "yyyy-MM-dd",
                        min_doc_count: 1
                       ]
                break
            case 'terms':
                aggregation = [
                        field: agg.column
                       ]
                break
        }

        parent."$aggType" aggregation
    }
}

println 'testing'

def query = DevOpsInsightQueryBuilder.build {
    filter {
        column 'test'
        matches 'testVal'
    }
    filter {
        column 'test2'
        exists()
    }

    group 'builds_time', {
        column 'endTime'
        by 'day'
    }

    group 'builds_outcome', {
        column 'buildStatus'
    }
}

//qb.exists('column2')

println 'printing: ' + query
