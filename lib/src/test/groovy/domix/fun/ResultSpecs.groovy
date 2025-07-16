package domix.fun

import spock.lang.Specification

class ResultSpecs extends Specification {
    def 'should map an OK value'() {
        given:
            def result = Result.ok('hello')
        when:
            def mappedValue = result
                .map { it.length() }
                .get()
        then:
            assert 5 == mappedValue
    }

    def 'should avoid to map an Error value'() {
        given:
            def result = Result.err('hello')
        when:
            result
                .map { 'WTF!' }
                .get()
        then:
            thrown(NoSuchElementException)
    }

    def 'should get a null on Error value'() {
        given:
            def result = Result.err('hello')
        when:
            def value = result
                .getOrNull()
        then:
            assert value == null
    }

    def 'should get the value'() {
        given:
            def result = Result.ok('hello')
        when:
            def value = result
                .getOrNull()
        then:
            assert value == 'hello'
    }

    def 'should get the error'() {
        given:
            def result = Result.err('hello')
        when:
            def value = result
                .getError()
        then:
            value == 'hello'
        when:
            Result.ok('hello')
                .getError()
        then:
            def ex = thrown(NoSuchElementException)
            ex.message == 'No error present.'
    }

    def 'should map error'() {
        given:
            def result = Result.err('foo')
        when:
            def mappedError = result
                .mapError { it.length() }
                .map { throw new RuntimeException('WTF') }
                .getError()
        then:
            mappedError == 3
        when:
            result = Result<String, String>.ok('foo')
            def error = result
                .mapError { '' }
                .getError()
        then:
            thrown(NoSuchElementException)
    }
}
