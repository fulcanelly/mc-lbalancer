{-# LANGUAGE RecordWildCards #-}
{-# LANGUAGE OverloadedStrings #-}
{-# LANGUAGE BlockArguments #-}  

module Main where

import Network.Simple.TCP
import Data.Time.Clock.POSIX (getPOSIXTime)
import Control.Applicative (Alternative(many))
import Data.ByteString (ByteString)
import Data.Maybe (fromJust)
import Control.Monad (forever, guard)

import Network.Socket (socketToHandle)

import System.IO
import System.Process

type Sec = Int

data Event
    = Empty
    | Few Int
    deriving Show 

data State 
    = Off 
    | On 
    | Waiting Double
    deriving Show 

data Context = Ctx {
        maxT :: Int 
        , diff :: Double
    } deriving Show 

stopServer = do
    
    putStrLn <$> (readProcess "tmux" ["kill-session", "-t", "main"] [])
    putStrLn "==> stopping"

startSever = do
    putStrLn <$> readProcess "sh" ["/home/minecraft/server/mine/start.sh" ] []
    putStrLn "==> starting"

maxWaitSec = 5 

update :: Context -> Event -> State -> IO State 
update Ctx{..} Empty event =
    case event of 
    Off -> pure Off
    On -> pure $ Waiting diff
    Waiting sec -> 
        if round sec > maxT then do 
            stopServer 
            pure Off 
        else
            pure $ Waiting (sec + diff) 
            
update _ (Few _) event =
    case event of 
    Off -> do
        startSever
        pure On
    _ -> pure On 


toEvent n = if n == 0 then Empty else Few n

fetchEvent = connect "localhost" "1344" onConn where
    onConn (socket, addr) = do
        h <- socketToHandle socket ReadWriteMode         
        sstate <- setupState h
        runConn h sstate =<< getTime

setupState h = do 
    h `hPutStrLn` "is_running"
    run <- hGetLine h
    pure $ case run of
        "true" -> On
        _ -> Off

runConn :: Handle -> State -> Double -> IO ()
runConn h state time = do 
    h `hPutStrLn` "get_online"
    event <- toEvent . read <$> hGetLine h 

    time' <- getTime
    let diff' = time' - time
    
    print state 
    putStrLn $ "got: " <> show event
    putStrLn $ "time took: " <> show diff'
   
    state <- update (Ctx maxWaitSec diff') event state  

    runConn h state time' 


getTime :: IO Double 
getTime = realToFrac <$> getPOSIXTime


main :: IO ()
main = do
    let time = round <$> getPOSIXTime
    fetchEvent
