package codes.domix.fun

import spock.lang.Specification

import java.util.concurrent.atomic.AtomicBoolean
import java.util.function.Function

/**
 * Spock specification class that tests the functionality of the {@code Result} class.
 * This class contains comprehensive tests to verify the behavior of monadic operations
 * and error handling implemented in the {@code Result} class.
 */
class ResultSpecs extends Specification {
    /**
     * Static mapper function used in several tests.
     * Concatenates the string " world!" to the input value.
     */
    def static mapper = new Function<String, String>() {

        @Override
        String apply(String message) {
            "$message world!"
        }
    }

    /**
     * Tests mapping functionality on an OK value.
     * Verifies that the mapping operation correctly transforms the value and maintains the OK state.
     */
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

    /**
     * Tests getOrElse functionality when the Result is OK.
     * Verifies that the original value is returned instead of the fallback.
     */
    def 'should verify getOrElse return value when ok'() {
        given:
            def result = Result.ok('hello')
        expect:
            result.getOrElse('fallback') == 'hello'
    }

    /**
     * Tests getOrElse functionality when the Result is Error.
     * Verifies that the fallback value is returned.
     */
    def 'should verify getOrElse return fallback when error'() {
        given:
            def result = Result.err('hello')
        expect:
            result.getOrElse('fallback') == 'fallback'
    }

    /**
     * Tests getOrElseGet functionality when the Result is OK.
     * Verifies that the original value is returned instead of the supplier's value.
     */
    def 'should verify getOrElseGet return value when ok'() {
        given:
            def result = Result.ok('hello')
        expect:
            result.getOrElseGet { 'fallback' } == 'hello'
    }

    /**
     * Tests getOrElseGet functionality when the Result is Error.
     * Verifies that the supplier's value is returned.
     */
    def 'should verify getOrElseGet return fallback when error'() {
        given:
            def result = Result.err('hello')
        expect:
            result.getOrElseGet { 'fallback' } == 'fallback'
    }

    /**
     * Tests peek operation on OK Result.
     * Verifies that the consumer is executed for OK values.
     */
    def 'should verify peek on ok'() {
        given:
            def result = Result.ok('hello')
            def touched = new AtomicBoolean(false)
        when:
            result.peek { touched.set(true) }
        then:
            touched.get()
    }

    /**
     * Tests peek operation on Error Result.
     * Verifies that the consumer is not executed for Error values.
     */
    def 'should verify peek on err'() {
        given:
            def result = Result.err('hello')
            def touched = new AtomicBoolean(false)
        when:
            result.peek { touched.set(true) }
        then:
            !touched.get()
    }

    /**
     * Tests peekError operation on OK Result.
     * Verifies that the error consumer is not executed for OK values.
     */
    def 'should verify peekError on ok'() {
        given:
            def result = Result.ok('hello')
            def touched = new AtomicBoolean(false)
        when:
            result.peekError { touched.set(true) }
        then:
            !touched.get()
    }

    /**
     * Tests peekError operation on Error Result.
     * Verifies that the error consumer is executed for Error values.
     */
    def 'should verify peekError on err'() {
        given:
            def result = Result.err('hello')
            def touched = new AtomicBoolean(false)
        when:
            result.peekError { touched.set(true) }
        then:
            touched.get()
    }

    /**
     * Tests that mapping is not performed on Error values.
     * Verifies that attempting to get the value throws NoSuchElementException.
     */
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

    /**
     * Tests getOrNull functionality on Error Result.
     * Verifies that null is returned for Error values.
     */
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

    /**
     * Tests getting value from OK Result.
     * Verifies that the correct value is returned.
     */
    def 'should get the value'() {
        given:
            def result = Result.ok('hello')
        when:
            def value = result
                .getOrNull()
        then:
            value == 'hello'
    }

    /**
     * Tests getting error from Error Result.
     * Verifies error retrieval behavior for both OK and Error Results.
     */
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

    /**
     * Tests error mapping functionality.
     * Verifies that error transformation works correctly.
     */
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

    /**
     * Tests filter operation with direct error value.
     * Verifies filtering behavior when predicate fails.
     */
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

    /**
     * Tests filter operation with passing predicate.
     * Verifies that the Result remains unchanged when predicate passes.
     */
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

    /**
     * Tests filter operation on Error Result.
     * Verifies that filtering doesn't affect Error Results.
     */
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

    /**
     * Tests filter operation with error mapping function.
     * Verifies filtering behavior with custom error mapping.
     */
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
                    /* intentionally left blank – side-effects verified elsewhere */
                }
        then:
            filtered.getError() == 'hello world!'
    }

    /**
     * Tests filter operation with passing predicate and mapping function.
     * Verifies that the Result remains unchanged when predicate passes.
     */
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

    /**
     * Tests filter operation on Error Result with mapping function.
     * Verifies that filtering doesn't affect Error Results.
     */
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

    /**
     * Tests match operation for OK Result.
     * Verifies that the success consumer is executed.
     */
    def 'should process match for ok'() {
        given:
            def result = Result.ok('dd')
            def touchedOk = new AtomicBoolean(false)
        when:
            result.match(
                { touchedOk.set(true) },
                { touchedOk.set(false) }
            )
        then:
            touchedOk.get()
    }

    /**
     * Tests match operation for Error Result.
     * Verifies that the error consumer is executed.
     */
    def 'should process match for error'() {
        given:
            def result = Result.err('dd')
            def touchedErr = new AtomicBoolean(false)
        when:
            result.match(
                { touchedErr.set(false) },
                { touchedErr.set(true) }
            )
        then:
            touchedErr.get()
    }

    /**
     * Tests flatMap operation chaining.
     * Verifies behavior of multiple flatMap operations.
     */
    def 'should flatMap'() {
        given:
            def result = someOperation('hello')

        when:
            def flatMapped = result
                .flatMap { anotherOperation(it) }
                .flatMap { anotherOperationWithError(it.length()) }
                .flatMap { someOperation(it) }
        then:
            flatMapped.isError()
    }

    /**
     * Helper method that simulates an operation returning a Result.
     */
    private static Result<String, Integer> someOperation(String input) {
        Result.ok(input)
    }

    /**
     * Helper method that simulates another operation returning a Result.
     */
    private static Result<String, Integer> anotherOperation(String input) {
        Result.ok(input)
    }

    /**
     * Helper method that simulates an operation always returning an Error Result.
     */
    private static Result<String, Integer> anotherOperationWithError(int input) {
        Result.err(input)
    }

    /**
     * Tests getOrThrow functionality for OK Result.
     * Verifies that the value is returned without throwing exception.
     */
    def 'should getOrThrow for ok value'() {
        given:
            def result = Result.ok('hello')
        expect:
            result.getOrThrow { new NullPointerException() } == 'hello'
    }

    /**
     * Tests getOrThrow functionality for Error Result.
     * Verifies that the specified exception is thrown.
     */
    def 'should getOrThrow for err'() {
        given:
            def result = Result.err('hello')
        when:
            result.getOrThrow { new NullPointerException() }
        then:
            thrown(NullPointerException)
    }

    /**
     * Tests fold operation on OK Result.
     * Verifies that the success mapping function is applied.
     */
    def 'should fold an ok value'() {
        given:
            def result = Result.ok('hello')
                .flatMap { someOperation(it) }
        when:
            def folded = result
                .fold(
                    { it },
                    { 'the error' }
                )
        then:
            folded == 'hello'
    }

    /**
     * Tests fold operation on Error Result.
     * Verifies that the error mapping function is applied.
     */
    def 'should fold an err value'() {
        given:
            def result = Result.err('hello')
                .flatMap { anotherOperation('foo') }
        when:
            def folded = result
                .fold(
                    { it },
                    { 'the error' }
                )
        then:
            folded == 'the error'
    }

    def 'should filter an OK try but does not pass default'() {
        given:
            def result = Try.of { 'hello' }
        when:
            def filteredResult = result.filter {
                it.length() >= 10
            }
        then:
            filteredResult.isFailure()
            filteredResult.getCause() instanceof IllegalArgumentException
    }

    def 'should filter an OK try but does not pass custom'() {
        given:
            def result = Try.of { 'hello' }
        when:
            def filteredResult = result.filter(
                {
                    it.length() >= 10
                },
                {
                    new RuntimeException('boom!')
                }
            )
        then:
            filteredResult.isFailure()
            filteredResult.getCause() instanceof RuntimeException
    }

    def 'should not apply filter since try is failed'() {
        given:
            def attempt = Try.of {
                throw new RuntimeException('Boom!')
            }
        when:
            def filteredAttempt = attempt.filter {
                true
            }
        then:
            filteredAttempt.isFailure()
            filteredAttempt.getCause() instanceof RuntimeException
    }

    def 'should pass filter validation'() {
        given:
            def result = Try.of {
                'hello'
            }
        when:
            def filteredResult = result.filter {
                it == 'hello'
            }
        then:
            filteredResult.isSuccess()
            filteredResult.get() == 'hello'
    }

    def 'should handle exception thrown by predicate'() {
        given:
            def result = Try.of { 'hello' }
        when:
            def filteredResult = result.filter {
                throw new RuntimeException('predicate failed')
            }
        then:
            filteredResult.isFailure()
            filteredResult.getCause() instanceof RuntimeException
            filteredResult.getCause().message == 'predicate failed'
    }
}
