require "java"
require "jruby"

import com.sporkmonger.rmud.Script

SCRIPT = Java::ComSporkmongerRmud::Script.get_script_instance(SCRIPT_UUID)
STD_RNET = SCRIPT.get_remote_network_stream
STD_LNET = SCRIPT.get_local_network_stream

require "date"
require "time"
require "sync"

SYNC = Sync.new
TRIGGERS = []
COMMANDS = []
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
    buffer = data.dup
    for command in COMMANDS
      pattern, options, block = command
      captures = buffer.scan(pattern)
      if captures.first != nil
        result = block.call(captures)
        if options[:replace]
          buffer.gsub!(pattern, result)
        end
      end
    end
    unless data.strip != "" && buffer.strip == ""
      STD_LNET.println(buffer.inspect[1...-1])
      self.send_to_remote(buffer)
    end
  end

  def self.process_from_remote(data)
    STD_RNET.println(data.inspect[1...-1])
    buffer = data.dup
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
  if block == nil
    raise ArgumentError, "Cannot create a trigger without a block."
  end
  TRIGGERS << [pattern, options, block]
end

def delete_trigger(name)
  TRIGGERS.delete_if do |(pattern, options, block)|
    options[:name] == name
  end
end

def substitute(pattern, replacement)
  block = lambda { |captures| replacement }
  TRIGGERS << [pattern, {:replace => true}, block]
end

def input(pattern, options={}, &block)
  COMMANDS << [pattern, options, block]
end

def command(pattern, options={}, &block)
  new_block = lambda { |captures| block.call(captures); "" }
  COMMANDS << [pattern, options.merge(:replace => true), new_block]
end

def alias_command(pattern, replacement)
  block = lambda { |captures| replacement }
  COMMANDS << [pattern, {:replace => true}, block]
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

command("reload") do |captures|
  SCRIPT.reload
end

command(/^eval (.*)$/) do |captures|
  display(eval(captures.flatten.first).inspect + "\n")
end

puts "Script loaded."
