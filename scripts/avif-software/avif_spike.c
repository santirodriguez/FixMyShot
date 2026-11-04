// SPDX-License-Identifier: GPL-3.0-or-later
// Standalone, fixed-fixture host/Android experiment. No user image input.
#include <avif/avif.h>
#include <stdint.h>
#include <inttypes.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <time.h>
#include <sys/resource.h>
#include <unistd.h>
#include <fcntl.h>

#define WIDTH 65
#define HEIGHT 67
#define MAX_OUTPUT_BYTES 16384

static const uint8_t COLORS[6][3] = {
  {64, 64, 64}, {128, 128, 128}, {192, 192, 192},
  {192, 64, 64}, {64, 192, 64}, {64, 64, 192}
};

static void source_pixel(int x, int y, uint8_t *out) {
  if (y < HEIGHT / 2) {
    memcpy(out, COLORS[x * 6 / WIDTH], 3);
  } else if (y < (HEIGHT * 3) / 4) {
    uint8_t v = (y % 16 == 0 || x % 32 == 0) ? 255 : 18;
    out[0] = out[1] = out[2] = v;
  } else if (x % 32 <= 1 && y % 16 <= 10) {
    out[0] = 20; out[1] = 100; out[2] = 220;
  } else {
    out[0] = out[1] = out[2] = 255;
  }
}

static uint64_t monotonic_ns(void) {
  struct timespec ts;
  return clock_gettime(CLOCK_MONOTONIC, &ts) == 0 ?
      (uint64_t) ts.tv_sec * 1000000000ULL + (uint64_t) ts.tv_nsec : 0;
}

static int write_exclusive(const char *path, const uint8_t *data, size_t size) {
  const int fd = open(path, O_CREAT | O_WRONLY | O_EXCL | O_CLOEXEC, 0600);
  if (fd < 0) return 7;
  int result = 0;
  size_t total = 0;
  while (total < size) {
    const ssize_t written = write(fd, data + total, size - total);
    if (written <= 0) { result = 8; break; }
    total += (size_t) written;
  }
  if (close(fd) != 0) result = 8;
  if (result) unlink(path);
  return result;
}

int main(int argc, char **argv) {
  if (argc != 2) {
    fprintf(stderr, "usage: avif_spike <new-private-file.avif>\n");
    return 2;
  }
  int rc = 0;
  uint64_t elapsed = 0;
  size_t encoded_bytes = 0;
  avifImage *image = avifImageCreate(WIDTH, HEIGHT, 8, AVIF_PIXEL_FORMAT_YUV444);
  if (!image) return 3;
  image->yuvRange = AVIF_RANGE_FULL;
  image->colorPrimaries = AVIF_COLOR_PRIMARIES_BT709;
  image->transferCharacteristics = AVIF_TRANSFER_CHARACTERISTICS_SRGB;
  image->matrixCoefficients = AVIF_MATRIX_COEFFICIENTS_BT709;
  avifRGBImage rgb;
  avifRGBImageSetDefaults(&rgb, image);
  rgb.format = AVIF_RGB_FORMAT_RGB;
  rgb.depth = 8;
  rgb.rowBytes = WIDTH * 3;
  rgb.pixels = malloc((size_t) HEIGHT * rgb.rowBytes);
  if (!rgb.pixels) { avifImageDestroy(image); return 4; }
  for (int y = 0; y < HEIGHT; ++y) {
    for (int x = 0; x < WIDTH; ++x) {
      source_pixel(x, y, rgb.pixels + (size_t) y * rgb.rowBytes + (size_t) x * 3);
    }
  }
  avifResult res = avifImageRGBToYUV(image, &rgb);
  free(rgb.pixels);
  if (res != AVIF_RESULT_OK) { avifImageDestroy(image); return 5; }
  avifEncoder *encoder = avifEncoderCreate();
  if (!encoder) { avifImageDestroy(image); return 6; }
  encoder->quality = 100;
  encoder->speed = 6;
  encoder->maxThreads = 2;
  avifRWData data = AVIF_DATA_EMPTY;
  uint64_t started = monotonic_ns();
  res = avifEncoderWrite(encoder, image, &data);
  elapsed = monotonic_ns() - started;
  if (res != AVIF_RESULT_OK || data.size < 20 || data.size > MAX_OUTPUT_BYTES) {
    rc = 9;
  } else {
    rc = write_exclusive(argv[1], data.data, data.size);
    if (!rc) encoded_bytes = data.size;
  }
  avifRWDataFree(&data);
  avifEncoderDestroy(encoder);
  avifImageDestroy(image);
  struct rusage usage;
  long rss_kib = getrusage(RUSAGE_SELF, &usage) == 0 ? usage.ru_maxrss : -1;
  printf("{\"fixture\":\"65x67\",\"avif_pixel_format\":\"YUV444\","
         "\"encoded_bytes\":%zu,\"encode_ns\":%" PRIu64 ","
         "\"ru_maxrss_kib\":%ld,\"status\":%d}\n",
         encoded_bytes, elapsed, rss_kib, rc);
  return rc;
}
