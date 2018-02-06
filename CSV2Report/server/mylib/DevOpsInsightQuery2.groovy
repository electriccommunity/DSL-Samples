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

    void group(final String name, @DelegatesTo(Group) final Closure config) {
        size(0)
        final Group group = new Group()
        group.name(name)
        group.with config
        aggregations.add(group)
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

        void by(final String interval) { this.interval = interval }
        void column(final String column) { this.column = column }
        void name(final String name) { this.name = name }
    }


    static def build(@DelegatesTo(DevOpsInsightQueryBuilder) final Closure config) {
        final DevOpsInsightQueryBuilder builder = new DevOpsInsightQueryBuilder()
        builder.with config
        builder.build1()
    }

    def addAggregation(def json, Map args) {

    }

    def build1() {
        def filtersList = buildFilters()
        def json = new JsonBuilder()
        def query = json {
            size size,
            query {
                bool {
                    filter filtersList
                }
            }
        }

        (new JsonBuilder(query)).toPrettyString()
    }

    protected def buildFilters() {
        this.filters.collect {
            switch (it.operation) {
                case 'term':
                    [(it.operation): [(it.column): it.value]]
                    break
                case 'exists':
                    [(it.operation): [field: it.column]]
                    break
            }
        }
    }

}

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
}

//qb.exists('column2')

println "printing: ** $query **"
