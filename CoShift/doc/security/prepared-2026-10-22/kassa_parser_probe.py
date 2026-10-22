"""Run only against the isolated source copy; all repositories are mocked."""
import asyncio
import importlib.metadata
import json
import os
import sys
from unittest.mock import patch

sys.path.insert(0, sys.argv[1])
from C_adapters import web_controller as web
from starlette.formparsers import FormParser, MultiPartParser

class FakeUseCase:
    def __init__(self, config): pass
    def execute(self, items): return []

web.artikel_repository.lade_konfiguration = lambda: {}
web.artikel_repository.lade_artikel_aus_string = lambda text: [object()]
web.artikel_repository.erzeuge_export_string = lambda *args: 'Name;ID\nTest;1\n'
web.ArtikelSyncUseCase = FakeUseCase

async def request(path, body=b'', content_type=None, method='POST'):
    messages = []
    consumed = False
    async def receive():
        nonlocal consumed
        if not consumed:
            consumed = True
            return {'type':'http.request', 'body':body, 'more_body':False}
        await asyncio.sleep(100)
        return {'type':'http.disconnect'}
    async def send(message): messages.append(message)
    headers=[(b'host',b'testserver')]
    if content_type: headers += [(b'content-type',content_type.encode()),(b'content-length',str(len(body)).encode())]
    scope={'type':'http','asgi':{'version':'3.0','spec_version':'2.4'},'http_version':'1.1','method':method,'scheme':'http','path':path,'raw_path':path.encode(),'query_string':b'','root_path':'','headers':headers,'server':('testserver',80),'client':('127.0.0.1',1234)}
    try:
        await asyncio.wait_for(web.app(scope,receive,send),10)
        start=next(x for x in messages if x['type']=='http.response.start')
        return {'status':start['status'],'security_headers':len([k for k,v in start['headers'] if k in [b'x-frame-options',b'x-content-type-options',b'content-security-policy']])}
    except Exception as exc:
        return {'exception':type(exc).__name__,'detail':str(exc)[:180]}

async def main():
    results={'versions':{x:importlib.metadata.version(x) for x in ['fastapi','starlette','python-multipart','anyio']},'tests':{}}
    calls=[]
    original=FormParser.parse
    async def traced(self):
        calls.append('FormParser.parse')
        return await original(self)
    with patch.object(FormParser,'parse',traced):
        for path in ['/api/process','/upload']:
            before=len(calls)
            results['tests'][path+' urlencoded']=await request(path,b'nur_ja=true','application/x-www-form-urlencoded')
            results['tests'][path+' urlencoded']['parser_called']=len(calls)>before
        results['tests']['urlencoded 1001 fields']=await request('/api/process','&'.join('x%d=v'%i for i in range(1001)).encode(),'application/x-www-form-urlencoded')
        results['tests']['urlencoded field over 1MiB']=await request('/api/process',b'x='+b'a'*(1024*1024+1),'application/x-www-form-urlencoded')
    for path in ['/api/process','/upload']:
        body=b'--probe\r\nContent-Disposition: form-data; name="file"; filename="synthetic.csv"\r\nContent-Type: text/csv\r\n\r\nName;ID\r\nTest;1\r\n--probe--\r\n'
        results['tests'][path+' valid upload']=await request(path,body,'multipart/form-data; boundary=probe')
        bad=body.replace(b'filename="synthetic.csv"',b'filename="synthetic.txt"')
        results['tests'][path+' invalid extension']=await request(path,bad,'multipart/form-data; boundary=probe')
    repeated=b'--probe\r\nContent-Disposition: form-data; name="file"; filename="synthetic.csv"\r\n'+b'X-Probe: a\r\n'*101+b'\r\nName;ID\r\nTest;1\r\n--probe--\r\n'
    results['tests']['multipart 102 headers']=await request('/api/process',repeated,'multipart/form-data; boundary=probe')
    results['tests']['index template']=await request('/',method='GET')
    if os.environ.get('KASSA_EXPECT_FIXED') == '1':
        expected = {
            '/api/process urlencoded':422, '/upload urlencoded':422,
            'urlencoded 1001 fields':400, 'urlencoded field over 1MiB':400,
            '/api/process valid upload':200, '/upload valid upload':200,
            '/api/process invalid extension':400, '/upload invalid extension':400,
            'multipart 102 headers':400, 'index template':200,
        }
        for case, status in expected.items():
            assert results['tests'][case].get('status') == status, (case, results['tests'][case])
            assert results['tests'][case].get('security_headers') == 3, case
        results['assertions'] = '10 cases passed'
    print(json.dumps(results,indent=2))

asyncio.run(main())
