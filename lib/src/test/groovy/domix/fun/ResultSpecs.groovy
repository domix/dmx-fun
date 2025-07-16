package domix.fun

import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Function

class ResultSpecs extends Specification {
    def static mapper = new Function<String, String>() {

        @Override
        String apply(String s) {
            return s + ' world!'
        }
    }

    def 'should map an OK value'() {
        given:
            def result = Result.ok('hello')
        when:
            def isOk = result.isOk()
            def mappedValue = result
                .map { it.length() }
                .get()
        then:
            5 == mappedValue
            isOk
    }

    def 'should verify getOrElse return value when ok'() {
        given:
            def result = Result.ok('hello')
        expect:
            result.getOrElse('fallback') == 'hello'
    }

    def 'should verify getOrElse return fallback when error'() {
        given:
            def result = Result.err('hello')
        expect:
            result.getOrElse('fallback') == 'fallback'
    }

    def 'should verify getOrElseGet return value when ok'() {
        given:
            def result = Result.ok('hello')
        expect:
            result.getOrElseGet { 'fallback' } == 'hello'
    }

    def 'should verify getOrElseGet return fallback when error'() {
        given:
            def result = Result.err('hello')
        expect:
            result.getOrElseGet { 'fallback' } == 'fallback'
    }

    def 'should verify peek on ok'() {
        given:
            def result = Result.ok('hello')
            def touched = new AtomicBoolean(false)
        when:
            result.peek { touched.set(true) }
        then:
            touched.get()
    }

    def 'should verify peek on err'() {
        given:
            def result = Result.err('hello')
            def touched = new AtomicBoolean(false)
        when:
            result.peek { touched.set(true) }
        then:
            !touched.get()
    }

    def 'should verify peekError on ok'() {
        given:
            def result = Result.ok('hello')
            def touched = new AtomicBoolean(false)
        when:
            result.peekError { touched.set(true) }
        then:
            !touched.get()
    }

    def 'should verify peekError on err'() {
        given:
            def result = Result.err('hello')
            def touched = new AtomicBoolean(false)
        when:
            result.peekError { touched.set(true) }
        then:
            touched.get()
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
            value == null
            result.isError()
    }

    def 'should get the value'() {
        given:
            def result = Result.ok('hello')
        when:
            def value = result
                .getOrNull()
        then:
            value == 'hello'
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
            result
                .mapError { '' }
                .getError()
        then:
            thrown(NoSuchElementException)
    }

    def 'should filter an ok and set provided value for error'() {
        given:
            def result = Result.ok('hello')
        when:
            def filtered = result
                .filter(
                    { input -> input.length() > 10 },
                    'error'
                )
        then:
            filtered.getError() == 'error'
    }

    def 'should filter an ok and ignore provided value for error'() {
        given:
            def result = Result.ok('hello world!')
        when:
            def filtered = result
                .filter(
                    { input -> input.length() > 10 },
                    'error'
                )
        then:
            filtered.get() == 'hello world!'
    }

    def 'should filter an err and ignore provided value for error'() {
        given:
            def result = Result.err('hello world!')
        when:
            def filtered = result
                .filter(
                    { input -> input.length() > 10 },
                    'error'
                )
        then:
            filtered.getError() == 'hello world!'
    }

    def 'should filter an ok and use provided function for error'() {
        given:
            def result = Result.ok('hello')
        when:

            def filtered = result
                .filter(
                    { input -> input.length() > 10 },
                    mapper
                )
                .peekError {
                    println it.getClass().name
                }
        then:
            filtered.getError() == 'hello world!'
    }

    def 'should filter an ok and ignore provided function for error'() {
        given:
            def result = Result.ok('hello world!')
        when:
            def filtered = result
                .filter(
                    { input -> input.length() > 10 },
                    mapper
                )
        then:
            filtered.get() == 'hello world!'
    }

    def 'should filter an err and ignore provided function for error'() {
        given:
            def result = Result.err('hello world!')
        when:
            def filtered = result
                .filter(
                    { input -> input.length() > 10 },
                    mapper
                )
        then:
            filtered.getError() == 'hello world!'
    }
}
