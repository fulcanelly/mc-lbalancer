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
import Control.Exception.Base (catch, SomeException)
import GHC.Conc.IO (threadDelay)

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
    putStrLn "==> stopping"

startSever = do
    putStrLn "==> starting"

maxWaitSec = 60 * 60

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


type TrackedState = State 
type FetchedState = State

adjust :: FetchedState -> TrackedState -> State 
adjust Off _ = Off

adjust On Off = On 
adjust _ s = s

toEvent n = if n == 0 then Empty else Few n

fetchEvent = connect "localhost" "1344" onConn where
    onConn (socket, addr) = do
        h <- socketToHandle socket ReadWriteMode         
        sstate <- getState h
        runConn h sstate =<< getTime

getState h = do 
    h `hPutStrLn` "is_running"
    run <- hGetLine h
    pure $ case run of
        "true" -> On
        _ -> Off

getOnline :: Handle -> IO Int
getOnline h = do
     h `hPutStrLn` "get_online"            
     read <$> hGetLine h
 

runConn :: Handle -> State -> Double -> IO ()
runConn h state time = do
    event <- toEvent <$> getOnline h

    time' <- getTime
    let diff' = time' - time
    
    print state 
    putStrLn $ "got: " <> show event
    putStrLn $ "time took: " <> show diff'
   
    state <- update (Ctx maxWaitSec diff') event state  
    state' <- adjust <$> getState h <*> pure state

    runConn h state' time' 


getTime :: IO Double 
getTime = realToFrac <$> getPOSIXTime

catchS :: IO a -> (SomeException -> IO a) -> IO a
catchS = catch

main :: IO ()
main = do
    fetchEvent
    `catchS` \x -> do
        print x 
        putStrLn "got excpetion. trying rerun"
        threadDelay $ 1000 * 3000
        main
