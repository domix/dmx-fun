package codes.domix.fun.example.site.gettingstarted;

import codes.domix.fun.Result;

class ResultExample {
    void main() {
        Result<Integer, String> result = Result.ok(123);
        result.map(i -> i + 1);  // Result.ok(124)
        result.isOk();                 // true
        result.get();                  // 124

        Result<Integer, String> failure = Result.err("error");
        failure.map(i -> i + 1);  // Result.err("error")
        failure.isError();              // true
        failure.getError();             // "error"
    }
}
