require "socket"

# emulate server 
def handle(socket)
  loop do 
    resp = socket.gets
    case resp.split
    in "get_online"
      socket.puts(rand 0..10)
    else
      puts "unknown request"
    end
    sleep(rand 0..10)
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


