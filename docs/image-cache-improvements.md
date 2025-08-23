# Image Cache Performance Improvements

## Overview

This update addresses issue #320 by implementing a robust image caching system for PDF documents in the Text Manager view.

## Problem

Previously, when working with large PDF document sets in the Text Manager:
- All images were extracted fresh on every document selection
- No memory limits could lead to OutOfMemory errors
- Repeated expensive image extraction operations

## Solution

### ImageCacheManager

A new `ImageCacheManager` using Google Guava's Cache provides:

- **Memory-bounded caching**: 100MB default limit with automatic LRU eviction
- **Smart key generation**: Images cached by (PDF path, page index, image index)
- **Automatic deduplication**: Prevents duplicate images in memory
- **Time-based expiration**: 1-hour cache expiration for memory management
- **Performance monitoring**: Cache statistics for debugging

### Performance Benefits

1. **Reduced Memory Usage**: Bounded cache prevents OOM errors
2. **Faster Document Switching**: Cached images load instantly
3. **Efficient Resource Management**: Automatic cleanup of unused images
4. **Better User Experience**: No delays when revisiting documents

### Usage

The caching is completely transparent to users. The Text Manager view will automatically:
- Cache images as they're extracted from PDFs
- Reuse cached images when switching between documents
- Manage memory usage automatically
- Clean up old images when memory limits are reached

### Monitoring

For debugging purposes, the cache provides:
- `getCacheStats()`: Detailed cache performance statistics
- `getCacheSize()`: Current number of cached images
- `getEstimatedMemoryUsage()`: Estimated memory usage in bytes
- `clearCache()`: Manual cache clearing if needed

## Configuration

The default 100MB memory limit can be adjusted by modifying the `DEFAULT_MEMORY_LIMIT_MB` constant in `ImageCacheManager.kt` if needed for specific deployment environments.