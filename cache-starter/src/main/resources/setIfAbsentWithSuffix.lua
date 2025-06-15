if redis.call("exists", KEYS[1]) == 0 or string.find(redis.call("get", KEYS[1]), ARGV[3]) then
    return redis.call("set", KEYS[1], ARGV[1] .. ARGV[3], "EX", ARGV[2])
else
    return nil
end