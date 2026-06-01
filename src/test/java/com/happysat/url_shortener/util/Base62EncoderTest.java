package com.happysat.url_shortener.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class Base62EncoderTest {

    @Nested
    @DisplayName("encode()")
    class Encode {

        @Test
        @DisplayName("zero returns '0'")
        void shouldEncodeZero() {
            assertEquals("0", Base62Encoder.encode(0));
        }

        @ParameterizedTest(name = "encode({0}) = \"{1}\"")
        @DisplayName("single-digit values (0-61) map to single characters")
        @CsvSource({
                "1,  1",
                "9,  9",
                "10, a",
                "35, z",
                "36, A",
                "61, Z"
        })
        void shouldEncodeSingleDigitValues(long id, String expected) {
            assertEquals(expected, Base62Encoder.encode(id));
        }

        @ParameterizedTest(name = "encode({0}) = \"{1}\"")
        @DisplayName("multi-digit values produce correct codes")
        @CsvSource({
                "62,    10",
                "63,    11",
                "124,   20",
                "125,   21",
                "3844,  100",
                "238327, ZZZ"
        })
        void shouldEncodeMultiDigitValues(long id, String expected) {
            assertEquals(expected, Base62Encoder.encode(id));
        }

        @Test
        @DisplayName("large value (Long.MAX_VALUE) does not throw")
        void shouldHandleLargeValue() {
            String result = Base62Encoder.encode(Long.MAX_VALUE);
            assertNotNull(result);
            assertFalse(result.isEmpty());
        }

        @ParameterizedTest(name = "encode({0}) returns empty string")
        @DisplayName("negative inputs return empty string (loop never executes)")
        @ValueSource(longs = {-1, -62, -100, Long.MIN_VALUE})
        void shouldReturnEmptyStringForNegativeInput(long id) {
            assertEquals("", Base62Encoder.encode(id));
        }
    }

    @Nested
    @DisplayName("decode()")
    class Decode {

        @Test
        @DisplayName("'0' decodes to 0")
        void shouldDecodeZero() {
            assertEquals(0, Base62Encoder.decode("0"));
        }

        @ParameterizedTest(name = "decode(\"{1}\") = {0}")
        @DisplayName("single characters decode to correct values")
        @CsvSource({
                "1,  1",
                "9,  9",
                "10, a",
                "35, z",
                "36, A",
                "61, Z"
        })
        void shouldDecodeSingleCharacters(long expected, String code) {
            assertEquals(expected, Base62Encoder.decode(code));
        }

        @ParameterizedTest(name = "decode(\"{1}\") = {0}")
        @DisplayName("multi-character codes decode correctly")
        @CsvSource({
                "62,     10",
                "125,    21",
                "3844,   100",
                "238327, ZZZ"
        })
        void shouldDecodeMultiCharacterCodes(long expected, String code) {
            assertEquals(expected, Base62Encoder.decode(code));
        }

        @Test
        @DisplayName("empty string decodes to 0")
        void shouldDecodeEmptyString() {
            assertEquals(0, Base62Encoder.decode(""));
        }

        @Test
        @DisplayName("null input throws NullPointerException")
        void shouldThrowOnNullInput() {
            assertThrows(NullPointerException.class, () -> Base62Encoder.decode(null));
        }

        @Test
        @DisplayName("invalid characters produce corrupted result (indexOf returns -1)")
        void shouldProduceCorruptedResultForInvalidChars() {
            long result = Base62Encoder.decode("!@#");
            assertTrue(result < 0, "invalid chars cause indexOf=-1, producing a negative result");
        }

        @Test
        @DisplayName("mixed valid and invalid characters produce corrupted result")
        void shouldProduceCorruptedResultForMixedInput() {
            long validOnly = Base62Encoder.decode("ab");
            long mixed = Base62Encoder.decode("a!b");
            assertNotEquals(validOnly, mixed);
        }

        @Test
        @DisplayName("whitespace characters produce corrupted result")
        void shouldProduceCorruptedResultForWhitespace() {
            long result = Base62Encoder.decode(" ");
            assertTrue(result < 0, "space is not in BASE62, indexOf returns -1");
        }
    }

    @Nested
    @DisplayName("round-trip encode → decode")
    class RoundTrip {

        @ParameterizedTest(name = "decode(encode({0})) = {0}")
        @DisplayName("decode reverses encode for known values")
        @ValueSource(longs = {0, 1, 9, 10, 61, 62, 125, 999, 3844, 238327, 1_000_000, Integer.MAX_VALUE})
        void shouldRoundTrip(long id) {
            assertEquals(id, Base62Encoder.decode(Base62Encoder.encode(id)));
        }

        @Test
        @DisplayName("decode reverses encode for Long.MAX_VALUE")
        void shouldRoundTripMaxLong() {
            long id = Long.MAX_VALUE;
            assertEquals(id, Base62Encoder.decode(Base62Encoder.encode(id)));
        }

        @Test
        @DisplayName("round-trip breaks for negative: encode(-5) is empty, decode('') is 0")
        void shouldNotRoundTripForNegative() {
            String encoded = Base62Encoder.encode(-5);
            assertEquals("", encoded);
            assertEquals(0, Base62Encoder.decode(encoded));
        }
    }
}
