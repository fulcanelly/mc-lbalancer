require "socket"

# emulate server 
def handle(socket)
  loop do 
    case socket.gets.chop.split
    in "get_online", *tail
      socket.puts(rand 0..1)
    else
      puts "unknown request"
    end
    rand(0..1) => pause
    puts "pause for #{pause}"
    sleep(pause)
  end
end

TCPServer.new(1344)
  .then do |serv|
    Enumerator.produce do 
      serv.accept
    end
    .lazy
    .each do |s|
      Ractor.new(s) do 
        handle _1
      end
    end
  end


