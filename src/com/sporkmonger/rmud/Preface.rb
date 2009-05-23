require "java"
require "jruby"

puts $:.inspect

import com.sporkmonger.rmud.Script

SCRIPT = Java::ComSporkmongerRmud::Script.get_script_instance(SCRIPT_UUID)

require "date"
require "time"
require "sync"

SYNC = Sync.new
TRIGGERS = []
COMMANDS = []
ALIASES = []
TIMERS = []
TIMER_THREAD = Thread.new do
  loop do
    timers.each_with_index do |(frequency, options, block, run_at), index|
      next if run_at + frequency > Time.now
      run_at = Time.now
      if options[:runs] && options[:runs] > 0
        options[:runs] -= 1
      end
      block.call
      if options[:runs] == 0
        SYNC.synchronize do
          timers.delete_at(index)
        end
        next
      end
      timers[index] = [frequency, options, block, run_at]
      Thread.pass
    end
  end
end
TIMER_THREAD.priority = -2

class IOProcessor
  def self.send_to_local(data)
    SCRIPT.get_bridge.send_to_local(data)
  end

  def self.send_to_remote(data)
    SCRIPT.get_bridge.send_to_remote(data)
  end

  def self.process_from_local(data)
    self.send_to_remote(data)
  end

  def self.process_from_remote(data)
    buffer = data
    for trigger in TRIGGERS
      pattern, options, block = trigger
      if options[:ansi] == nil
        # Default to ansi off if we are not replacing
        # Default to ansi on if we are replacing
        options[:ansi] = !!options[:replace]
      end
      if options[:ansi]
        captures = buffer.scan(pattern)
      else
        captures = strip_ansi(buffer).scan(pattern)
      end
      if captures.first != nil
        result = block.call(captures)
        if options[:replace]
          if options[:ansi]
            buffer.gsub!(pattern, result)
          else
            buffer = strip_ansi(buffer).gsub(pattern, result)
          end
        end
      end
    end
    self.send_to_local(buffer)
  end
end

class Numeric
  if !defined?(days)
    def days
      self * 24.hours
    end
    alias_method :day, :days
  end

  if !defined?(hours)
    def hours
      self * 60.minutes
    end
    alias_method :hour, :hours
  end

  if !defined?(minutes)
    def minutes
      self * 60.seconds
    end
    alias_method :minute, :minutes
  end

  if !defined?(seconds)
    def seconds
      self
    end
    alias_method :second, :seconds
  end
end

def transmit(data)
  IOProcessor.send_to_remote(data)
end

def display(data)
  IOProcessor.send_to_local(data)
end

def trigger(pattern, options={}, &block)
  TRIGGERS << [pattern, options, block]
end

def timer(frequency, options={}, &block)
  SYNC.synchronize do
    TIMERS << [frequency, options, block, Time.now]
  end
end

def strip_ansi(string)
  string.gsub(/((\x9B|\x1B\[)(\d+;)*(\d+)?[mABCDEFGHJKSTfnsu])/, "")
end

def dice(sides=6, count=1)
  (1..count).inject(0) { |sum, _| sum += (rand(sides) + 1) }
end
