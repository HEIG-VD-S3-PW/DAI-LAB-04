package ch.heigvd.bdr.controllers;

import io.javalin.http.Context;
import io.javalin.http.NotModifiedResponse;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class UtilsController {
    public static LocalDateTime getLastModifiedHeader(Context ctx) {
        String ifModifiedSinceHeader = ctx.header("If-Modified-Since");
        LocalDateTime lastKnownModification = null;
        if (ifModifiedSinceHeader != null) {
            try {
                // Parse the If-Modified-Since header (RFC 1123 format)
                ZonedDateTime headerDateTime = ZonedDateTime.parse(
                        ifModifiedSinceHeader, DateTimeFormatter.RFC_1123_DATE_TIME
                );

                // Convert to LocalDateTime for comparison with your cache
                lastKnownModification = headerDateTime.toLocalDateTime();
            } catch (Exception e) {
                ctx.status(400).json(Map.of("message", "Invalid 'If-Modified-Since' header format."));
                return null;
            }
        }
        System.out.println("Date converted: " + lastKnownModification);
        return lastKnownModification;
    }

    public static boolean isModifiedSince(LocalDateTime headerTime, LocalDateTime cacheTime) {
        if (headerTime == null || cacheTime == null) {
            return false;
        }
        // Truncate both times to seconds for comparison
        LocalDateTime truncatedHeaderTime = headerTime.withNano(0);
        LocalDateTime truncatedCacheTime = cacheTime.withNano(0);

        return truncatedHeaderTime.equals(truncatedCacheTime);
    }

    public static void sendResponse(Context ctx, ConcurrentHashMap<Integer, LocalDateTime> cache, Integer key){
        LocalDateTime now;
        if(cache.containsKey(key)) {
            now = cache.get(key);
        }
        else{
            now = LocalDateTime.now();
            cache.put(key, now);
        }
        ctx.header("Last-Modified", now.toString());
    }

    public static void checkModif(Context ctx, ConcurrentHashMap<Integer, LocalDateTime> cache, Integer id){
        LocalDateTime lastKnownModification = UtilsController.getLastModifiedHeader(ctx);

        if(lastKnownModification == null){
            return ;
        }

        if(UtilsController.isModifiedSince(cache.get(id), lastKnownModification)) {
            throw new NotModifiedResponse();
        }
    }
}
